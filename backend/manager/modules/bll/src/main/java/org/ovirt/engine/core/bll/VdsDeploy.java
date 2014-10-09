package org.ovirt.engine.core.bll;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import javax.naming.TimeLimitExceededException;

import org.apache.commons.lang.StringUtils;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VDSType;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.LocalConfig;
import org.ovirt.engine.core.utils.NetworkUtils;
import org.ovirt.engine.core.utils.archivers.tar.CachedTar;
import org.ovirt.engine.core.utils.crypt.OpenSSHUtils;
import org.ovirt.engine.core.utils.hostinstall.OpenSslCAWrapper;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.ssh.EngineSSHDialog;
import org.ovirt.engine.core.utils.ssh.SSHDialog;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

import org.ovirt.otopi.constants.BaseEnv;
import org.ovirt.otopi.constants.Confirms;
import org.ovirt.otopi.constants.CoreEnv;
import org.ovirt.otopi.constants.NetEnv;
import org.ovirt.otopi.constants.Queries;
import org.ovirt.otopi.constants.SysEnv;
import org.ovirt.otopi.dialog.Event;
import org.ovirt.otopi.dialog.MachineDialogParser;
import org.ovirt.otopi.dialog.SoftError;
import org.ovirt.ovirt_host_deploy.constants.GlusterEnv;
import org.ovirt.ovirt_host_deploy.constants.VdsmEnv;

/**
 * Host deploy implementation.
 * Executed if:
 * <ul>
 * <li>Host install.</li>
 * <li>Host re-install.</li>
 * <li>Node install.</li>
 * <li>Node approve.</li>
 * </ul>
 *
 * The deploy process is done via the ovirt-host-deploy component using
 * otopi machine dialog interface, refer to otopi documentation.
 * The installer environment is set according to the ovirt-host-deploy
 * documentation.
 */
public class VdsDeploy implements SSHDialog.Sink {

    private static final int THREAD_JOIN_TIMEOUT = 20 * 1000; // milliseconds
    private static final String IPTABLES_CUSTOM_RULES_PLACE_HOLDER = "@CUSTOM_RULES@";
    private static final String BOOTSTRAP_CUSTOM_ENVIRONMENT_PLACE_HOLDER = "@ENVIRONMENT@";

    private static final Log log = LogFactory.getLog(VdsDeploy.class);
    private static CachedTar s_deployPackage;

    private SSHDialog.Control _control;
    private Thread _thread;
    private EngineSSHDialog _dialog;
    private MachineDialogParser _parser;
    private final InstallerMessages _messages;

    private VDS _vds;
    private boolean _isNode = false;
    private boolean _reboot = false;
    private Exception _failException = null;
    private boolean _resultError = false;
    private boolean _goingToReboot = false;
    private boolean _aborted = false;
    private boolean _installIncomplete = false;

    private String _certificate;
    private String _iptables = "";

    /**
     * set vds object with unique id.
     * Check if vdsmid is unique, if not, halt installation, otherwise
     * update the vds object.
     * @param vdsmid unique id read from host.
     */
    private void _setVdsmId(String vdsmid) {
        if (vdsmid == null) {
            throw new SoftError("Cannot acquire node id");
        }

        log.infoFormat(
            "Host {0} reports unique id {1}",
            _vds.gethost_name(),
            vdsmid
        );

        final List<VDS> list = LinqUtils.filter(
            DbFacade.getInstance().getVdsDao().getAllWithUniqueId(vdsmid),
            new Predicate<VDS>() {
                @Override
                public boolean eval(VDS vds) {
                    return !vds.getId().equals(_vds.getId());
                }
            }
        );

        if (!list.isEmpty()) {
            final StringBuilder hosts = new StringBuilder(1024);
            for (VDS v : list) {
                if (hosts.length() > 0) {
                    hosts.append(", ");
                }
                hosts.append(v.getvds_name());
            }

            log.errorFormat(
                "Host {0} reports duplicate unique id {1} of following hosts {2}",
                _vds.gethost_name(),
                vdsmid,
                hosts
            );
            throw new SoftError(
                String.format(
                    "Host %1$s reports unique id which already registered for %2$s",
                    _vds.gethost_name(),
                    hosts
                )
            );
        }

        log.infoFormat("Assigning unique id {0} to Host {1}", vdsmid, _vds.gethost_name());
        _vds.setUniqueId(vdsmid);

        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                DbFacade.getInstance().getVdsStaticDao().update(_vds.getStaticData());
                return null;
            }
        });
    }

    /**
     * Set host to be node.
     */
    private void _setNode() {
        _isNode = true;

        _vds.setvds_type(VDSType.oVirtNode);

        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                DbFacade.getInstance().getVdsStaticDao().update(_vds.getStaticData());
                return null;
            }
        });
    }

    /**
     * Set vds object status.
     * For this simple task, no need to go via command mechanism.
     * @param status new status.
     */
    private void _setVdsStatus(VDSStatus status) {
        _vds.setstatus(status);

        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                DbFacade.getInstance().getVdsDynamicDao().update(_vds.getDynamicData());
                return null;
            }
        });
    }

    /**
     * Return the engine ssh public key to install on host.
     * @return ssh public key.
     */
    private static String _getEngineSSHPublicKey() {
        final String keystoreFile = Config.<String>GetValue(ConfigValues.keystoreUrl);
        final String alias = Config.<String>GetValue(ConfigValues.CertAlias);
        final char[] password = Config.<String>GetValue(ConfigValues.keystorePass).toCharArray();

        InputStream in = null;
        try {
            in = new FileInputStream(keystoreFile);
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(in, password);

            final Certificate cert = ks.getCertificate(alias);
            if (cert == null) {
                throw new KeyStoreException(
                    String.format(
                        "Failed to find certificate store '%1$s' using alias '%2$s'",
                        keystoreFile,
                        alias
                    )
                );
            }

            return OpenSSHUtils.getKeyString(
                cert.getPublicKey(),
                Config.<String>GetValue(ConfigValues.SSHKeyAlias)
            );
        }
        catch (Exception e) {
            throw new RuntimeException(
                String.format(
                    "Failed to decode own public key from store '%1$s' using alias '%2$s'",
                    keystoreFile,
                    alias
                ),
                e
            );
        }
        finally {
            Arrays.fill(password, '*');
            if (in != null) {
                try {
                    in.close();
                }
                catch(IOException e) {
                    log.error("Cannot close key store", e);
                }
            }
        }
    }

    /**
     * Construct iptables to send.
     */
    private String _getIpTables() {
        VDSGroup vdsGroup = DbFacade.getInstance().getVdsGroupDao().get(
            _vds.getvds_group_id()
        );

        String ipTablesConfig = Config.<String> GetValue(ConfigValues.IPTablesConfig);

        String serviceIPTablesConfig = "";
        if (vdsGroup.supportsVirtService()) {
            serviceIPTablesConfig += Config.<String> GetValue(ConfigValues.IPTablesConfigForVirt);
        }
        if (vdsGroup.supportsGlusterService()) {
            serviceIPTablesConfig += Config.<String> GetValue(ConfigValues.IPTablesConfigForGluster);
        }

        ipTablesConfig = ipTablesConfig.replace(
            IPTABLES_CUSTOM_RULES_PLACE_HOLDER,
            serviceIPTablesConfig
        );

        return ipTablesConfig;
    }

    /*
     * Customization dialog.
     */

    /**
     * Customization tick.
     */
    private int _customizationIndex = 0;
    /**
     * Customization aborting.
     */
    private boolean _customizationShouldAbort = false;
    /**
     * Customization vector.
     * This is tick based vector, every event execute the next
     * tick.
     */
    private final Callable[] _customizationDialog = new Callable[] {
        new Callable<Object>() { public Object call() throws Exception {
            if (
                (Boolean)_parser.cliEnvironmentGet(
                    VdsmEnv.OVIRT_NODE
                )
            ) {
                _messages.post(
                    InstallerMessages.Severity.INFO,
                    "Host is ovirt-node"
                );
                _setNode();
            }
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _messages.post(
                InstallerMessages.Severity.INFO,
                String.format(
                    "Logs at host located at: '%1$s'",
                    _parser.cliEnvironmentGet(
                        CoreEnv.LOG_FILE_NAME
                    )
                )
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                SysEnv.CLOCK_SET,
                true
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                NetEnv.SSH_ENABLE,
                true
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                NetEnv.SSH_KEY,
                _getEngineSSHPublicKey().replace("\n", "")
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                NetEnv.IPTABLES_ENABLE,
                _iptables.length() > 0
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            if (_iptables.length() == 0) {
                _parser.cliNoop();
            }
            else {
                _parser.cliEnvironmentSet(
                    NetEnv.IPTABLES_RULES,
                    _iptables.split("\n")
                );
            }
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _setVdsmId((String)_parser.cliEnvironmentGet(VdsmEnv.VDSM_ID));
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                String.format(
                    "%svars/ssl",
                    VdsmEnv.CONFIG_PREFIX
                ),
                Config.<Boolean> GetValue(ConfigValues.UseSecureConnectionWithServers).toString()
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                String.format(
                    "%saddresses/management_port",
                    VdsmEnv.CONFIG_PREFIX
                ),
                Integer.toString(_vds.getport())
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                VdsmEnv.ENGINE_HOST,
                LocalConfig.getInstance().getHost()
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                VdsmEnv.ENGINE_PORT,
                LocalConfig.getInstance().getExternalHttpPort()
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                VdsmEnv.MANAGEMENT_BRIDGE_NAME,
                NetworkUtils.getEngineNetwork()
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            String minimal = Config.<String> GetValue(ConfigValues.BootstrapMinimalVdsmVersion);
            if (minimal.trim().length() == 0) {
                _parser.cliNoop();
            }
            else {
                _parser.cliEnvironmentSet(
                    VdsmEnv.VDSM_MINIMUM_VERSION,
                    minimal
                );
            }
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            VDSGroup vdsGroup = DbFacade.getInstance().getVdsGroupDao().get(
                _vds.getvds_group_id()
            );
            _parser.cliEnvironmentSet(
                VdsmEnv.CHECK_VIRT_HARDWARE,
                vdsGroup.supportsVirtService()
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                VdsmEnv.CERTIFICATE_ENROLLMENT,
                org.ovirt.ovirt_host_deploy.constants.Const.CERTIFICATE_ENROLLMENT_INLINE
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            VDSGroup vdsGroup = DbFacade.getInstance().getVdsGroupDao().get(
                _vds.getvds_group_id()
            );
            _parser.cliEnvironmentSet(
                GlusterEnv.ENABLE,
                vdsGroup.supportsGlusterService()
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            /**
             * Legacy logic
             * Force reboot only if not node.
             */
            boolean reboot = _reboot && !_isNode;
            if (reboot) {
                _messages.post(
                    InstallerMessages.Severity.INFO,
                    "Enfocing host reboot"
                );
            }
            _parser.cliEnvironmentSet(
                org.ovirt.ovirt_host_deploy.constants.CoreEnv.FORCE_REBOOT,
                reboot
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliInstall();
            return null;
        }},
    };
    /**
     * Execute the next customization vector entry.
     */
    private void _nextCustomizationEntry() throws Exception {
        try {
            if (_customizationShouldAbort) {
                _parser.cliAbort();
            }
            else {
                _customizationDialog[_customizationIndex++].call();
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Protocol violation", e);
        }
        catch (SoftError e) {
            log.errorFormat(
                "Soft error during host {0} customization dialog",
                _vds.gethost_name(),
                e
            );
            _failException = e;
            _customizationShouldAbort = true;
        }
    }

    /*
     * Termination dialog.
     */

    /**
     * Termination dialog tick.
     */
    private int _terminationIndex = 0;
    /**
     * Termination vector.
     * This is tick based vector, every event execute the next
     * tick.
     */
    private final Callable[] _terminationDialog = new Callable[] {
        new Callable<Object>() { public Object call() throws Exception {
            _resultError = (Boolean)_parser.cliEnvironmentGet(
                BaseEnv.ERROR
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _aborted = (Boolean)_parser.cliEnvironmentGet(
                BaseEnv.ABORTED
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _installIncomplete = (Boolean)_parser.cliEnvironmentGet(
                org.ovirt.ovirt_host_deploy.constants.CoreEnv.INSTALL_INCOMPLETE
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            if (_resultError || !_installIncomplete) {
                _parser.cliNoop();
            }
            else {
                String[] msgs = (String[])_parser.cliEnvironmentGet(
                    org.ovirt.ovirt_host_deploy.constants.CoreEnv.INSTALL_INCOMPLETE_REASONS
                );
                _messages.post(
                    InstallerMessages.Severity.WARNING,
                    "Installation is incomplete, manual intervention is required"
                );
                for (String m : msgs) {
                    _messages.post(
                        InstallerMessages.Severity.WARNING,
                        m
                    );
                }
            }
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _goingToReboot = (Boolean)_parser.cliEnvironmentGet(
                SysEnv.REBOOT
            );
            if (_goingToReboot) {
                _messages.post(
                    InstallerMessages.Severity.INFO,
                    "Reboot scheduled"
                );
            }
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            OutputStream os = null;
            File logFile = new File(
                LocalConfig.getInstance().getLogDir(),
                String.format(
                    "%1$s%2$sovirt-%3$s-%4$s.log",
                    "host-deploy",
                    File.separator,
                    new SimpleDateFormat("yyyyMMddHHmmss").format(
                        Calendar.getInstance().getTime()
                    ),
                    _vds.gethost_name()
                )
            );
            _messages.post(
                InstallerMessages.Severity.INFO,
                String.format(
                    "Retrieving installation logs to: '%1$s'",
                    logFile
                )
            );
            try {
                os = new FileOutputStream(logFile);
                _parser.cliDownloadLog(os);
            }
            catch (IOException e) {
                throw e;
            }
            catch (Exception e) {
                log.error("Unexpected exception", e);
                throw new RuntimeException(e);
            }
            finally {
                if (os != null) {
                    try {
                        os.close();
                    }
                    catch (IOException e) {
                        log.error("cannot close log file", e);
                    }
                }
            }
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliEnvironmentSet(
                CoreEnv.LOG_REMOVE_AT_EXIT,
                true
            );
            return null;
        }},
        new Callable<Object>() { public Object call() throws Exception {
            _parser.cliQuit();
            return null;
        }},
    };
    /**
     * Execute the next termination vector entry.
     */
    private void _nextTerminationEntry() throws Exception {
        try {
            _terminationDialog[_terminationIndex++].call();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Protocol violation", e);
        }
    }

    /**
     * Dialog implementation.
     * Handle events incoming from host.
     */
    private void _threadMain() {
        try {
            boolean terminate = false;

            while(!terminate) {
                Event.Base bevent = _parser.nextEvent();

                log.debugFormat(
                    "Installation of {0}: Event {1}",
                    _vds.gethost_name(),
                    bevent
                );

                if (bevent instanceof Event.Terminate) {
                    terminate = true;
                }
                else if (bevent instanceof Event.Log) {
                    Event.Log event = (Event.Log)bevent;
                    InstallerMessages.Severity severity;
                    switch (event.severity) {
                    case INFO:
                        severity = InstallerMessages.Severity.INFO;
                        break;
                    case WARNING:
                        severity = InstallerMessages.Severity.WARNING;
                        break;
                    default:
                        severity = InstallerMessages.Severity.ERROR;
                        break;
                    }
                    _messages.post(severity, event.record);
                }
                else if (bevent instanceof Event.Confirm) {
                    Event.Confirm event = (Event.Confirm)bevent;

                    if (Confirms.GPG_KEY.equals(event.what)) {
                        _messages.post(InstallerMessages.Severity.WARNING, event.description);
                        event.reply = true;
                    }
                    else if (org.ovirt.ovirt_host_deploy.constants.Confirms.DEPLOY_PROCEED.equals(event.what)) {
                        event.reply = true;
                    }
                    else {
                        log.warnFormat(
                            "Installation of {0}: Not confirming {1}: ${2}",
                            _vds.gethost_name(),
                            event.what,
                            event.description
                        );
                    }

                    _parser.sendResponse(event);
                }
                else if (bevent instanceof Event.QueryString) {
                    Event.QueryString event = (Event.QueryString)bevent;

                    if (Queries.CUSTOMIZATION_COMMAND.equals(event.name)) {
                        _nextCustomizationEntry();
                    }
                    else if (Queries.TERMINATION_COMMAND.equals(event.name)) {
                        _nextTerminationEntry();
                    }
                    else {
                        throw new Exception(
                            String.format(
                                "Unexpected query %1$s",
                                event
                            )
                        );
                    }
                }
                else if (bevent instanceof Event.QueryValue) {
                    Event.QueryValue event = (Event.QueryValue)bevent;

                    if (Queries.TIME.equals(event.name)) {
                        _messages.post(
                            InstallerMessages.Severity.INFO,
                            "Setting time"
                        );
                        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssZ");
                        format.setTimeZone(TimeZone.getTimeZone("UTC"));
                        event.value = format.format(Calendar.getInstance().getTime());
                    }
                    else {
                        event.abort = true;
                    }
                    _parser.sendResponse(event);
                }
                else if (bevent instanceof Event.QueryMultiString) {
                    Event.QueryMultiString event = (Event.QueryMultiString)bevent;

                    if (org.ovirt.ovirt_host_deploy.constants.Queries.CERTIFICATE_CHAIN.equals(event.name)) {
                        event.value = (
                            OpenSslCAWrapper.getCACertificate() +
                            _certificate
                        ).split("\n");
                        _parser.sendResponse(event);
                    }
                    else {
                        event.abort = true;
                        _parser.sendResponse(event);
                    }
                }
                else if (bevent instanceof Event.DisplayMultiString) {
                    Event.DisplayMultiString event = (Event.DisplayMultiString)bevent;

                    if (org.ovirt.ovirt_host_deploy.constants.Displays.CERTIFICATE_REQUEST.equals(event.name)) {
                        _messages.post(
                            InstallerMessages.Severity.INFO,
                            "Enrolling certificate"
                        );
                        _certificate = OpenSslCAWrapper.SignCertificateRequest(
                            StringUtils.join(event.value, "\n"),
                            _vds.gethost_name(),
                            _vds.gethost_name()
                        );
                    }
                }
                else {
                    throw new SoftError(
                        String.format(
                            "Unexpected event '%1$s'",
                            bevent
                        )
                    );
                }
            }
        }
        catch (Exception e) {
            _failException = e;
            log.error("Error during deploy dialog", e);
            _control.disconnect();
        }
    }

    /*
     * Constructor.
     * @param vds vds to install.
     */
    public VdsDeploy(VDS vds) {
        _vds = vds;

        _messages = new InstallerMessages(_vds);
        _dialog = new EngineSSHDialog();
        _parser = new MachineDialogParser();
        _thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    _threadMain();
                }
            },
            "VdsDeploy"
        );

        if (s_deployPackage == null) {
            s_deployPackage = new CachedTar(
                new File(
                    LocalConfig.getInstance().getCacheDir(),
                    Config.<String> GetValue(ConfigValues.BootstrapPackageName)
                ),
                new File(Config.<String> GetValue(ConfigValues.BootstrapPackageDirectory))
            );
        }
    }

    /**
     * Destructor.
     */
    @Override
    public void finalize() {
        close();
    }

    /**
     * Release resources.
     */
    public void close() {
        stop();
        if (_dialog != null) {
            _dialog.disconnect();
            _dialog = null;
        }
    }

    /**
     * Set reboot.
     * @param reboot reboot.
     */
    public void setReboot(boolean reboot) {
        _reboot = reboot;
    }

    /**
     * Set user.
     * @param user user.
     */
    public void setUser(String user) {
        _dialog.setUser(user);
    }

    /**
     * Set key pair.
     * @param keyPair key pair.
     */
    public void setKeyPair(KeyPair keyPair) {
        _dialog.setKeyPair(keyPair);
    }

    /**
     * Use engine default key pairs.
     */
    public void useDefaultKeyPair() throws KeyStoreException {
        _dialog.useDefaultKeyPair();
    }

    /**
     * Set password.
     * @param password password.
     */
    public void setPassword(String password) {
        _dialog.setPassword(password);
    }

    /**
     * Enable firewall setup.
     * @param doFirewall enable.
     */
    public void setFirewall(boolean doFirewall) {
        if (doFirewall) {
            _iptables = _getIpTables();
        }
    }

    /**
     * Main method.
     * Execute the command and initiate the dialog.
     */
    public void execute() throws Exception {
        InputStream in = null;
        try {
            _setVdsStatus(VDSStatus.Installing);

            _dialog.setHost(_vds.gethost_name());
            _dialog.connect();
            _messages.post(
                InstallerMessages.Severity.INFO,
                String.format(
                    "Connected to host %1$s with SSH key fingerprint: %2$s",
                    _vds.gethost_name(),
                    _dialog.getHostFingerprint()
                )
            );
            _dialog.authenticate();

            String command = Config.<String> GetValue(ConfigValues.BootstrapCommand);

            // in future we should set here LANG, LC_ALL
            command = command.replace(
                BOOTSTRAP_CUSTOM_ENVIRONMENT_PLACE_HOLDER,
                ""
            );

            log.infoFormat(
                "Installation of {0}. Executing command via SSH {1} < {2}",
                _vds.gethost_name(),
                command,
                s_deployPackage.getFileNoUse()
            );

            in = new FileInputStream(s_deployPackage.getFile());

            _dialog.executeCommand(
                this,
                command,
                new InputStream[] {in}
            );

            if (_failException != null) {
                throw _failException;
            }

            if (_resultError) {
                // This is unlikeley as the ssh command will exit with failure.
                throw new RuntimeException(
                    "Installation failed, please refer to installation logs"
                );
            }
            else if (_goingToReboot) {
                _setVdsStatus(VDSStatus.Reboot);
            }
            else if (_installIncomplete) {
                _setVdsStatus(VDSStatus.InstallFailed);
            }
            else {
                _setVdsStatus(VDSStatus.NonResponsive);
            }
        }
        catch (TimeLimitExceededException e){
            log.errorFormat(
                "Timeout during host {0} install",
                _vds.gethost_name(),
                e
            );
            _messages.post(
                InstallerMessages.Severity.ERROR,
                "Processing stopped due to timeout"
            );
            _setVdsStatus(VDSStatus.InstallFailed);
            throw e;
        }
        catch(Exception e) {
            log.errorFormat(
                "Error during host {0} install",
                _vds.gethost_name(),
                e
            );
            _setVdsStatus(VDSStatus.InstallFailed);

            if (_failException == null) {
                throw e;
            }
            else {
                log.errorFormat(
                    "Error during host {0} install, prefering first exception",
                    _vds.gethost_name(),
                    _failException
                );
                throw _failException;
            }
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    log.error("Cannot close deploy package", e);
                }
            }
        }
    }

    /*
     * SSHDialog.Sink
     */

    @Override
    public void setControl(SSHDialog.Control control) {
        _control = control;
    }

    @Override
    public void setStreams(InputStream incoming, OutputStream outgoing) {
        _parser.setStreams(incoming, outgoing);
    }

    @Override
    public void start() {
        _thread.start();
    }

    @Override
    public void stop() {
        if (_thread != null) {
            /*
             * We cannot just interrupt the thread as the
             * implementation of jboss connection pooling
             * drops the connection when interrupted.
             * As we may have log events pending to be written
             * to database, we wait for some time for thread
             * complete before interrupting.
             */
            try {
                _thread.join(THREAD_JOIN_TIMEOUT);
            }
            catch (InterruptedException e) {
                log.error("interrupted", e);
            }
            if (_thread.isAlive()) {
                _thread.interrupt();
                while(true) {
                    try {
                        _thread.join();
                        break;
                    }
                    catch (InterruptedException e) {}
                }
            }
            _thread = null;
        }
    }
}
