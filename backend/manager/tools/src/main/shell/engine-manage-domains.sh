#!/bin/bash
#
# This script is designed to run the manage domains utility.
# The tool's configuration should be under the /etc directory.
#

# Load the prolog:
. "$(dirname "$(readlink -f "$0")")"/engine-prolog.sh

CONF_DIR="${ENGINE_ETC}/engine-manage-domains"
CONF_FILE="${CONF_DIR}/engine-manage-domains.conf"

found=0
for ((i=1; i<=$# && ! found; i++))
do
        var="${!i}"
        next=$[$i+1]
        next="${!next}"

        if [ "-c" == "${var}" ]; then
                CONF_FILE="${next}"
                found=1
        elif [ `echo "${var}" | grep -i '\-configFile\='` ]; then
                candidate=${var#-configFile=}
                if [ -s $candidate ]; then
                        CONF_FILE=$candidate
                else
                        die "Error: Alternate conf file $candidate is either empty or does not exist"
                fi
                found=1
        fi
done

if [ ! -s $CONF_FILE ]; then
		die "Error: Configuration file $CONF_FILE is either empty or does not exist"
fi


. $CONF_FILE


usage () {
        printf "engine-manage-domains: add/edit/delete/validate/list domains\n"
        printf "USAGE:\n"
        printf "\tengine-manage-domains -action=ACTION [-domain=DOMAIN -provider=PROVIDER -user=USER -passwordFile=PASSWORD_FILE -interactive -configFile=PATH -addPermissions -forceDelete -ldapServers=LDAP_SERVERS] -report\n"
        printf "Where:\n"
        printf "\tACTION             action to perform (add/edit/delete/validate/list). See details below.\n"
        printf "\tDOMAIN             	(mandatory for add, edit and delete) the domain you wish to perform the action on.\n"
        printf "\tPROVIDER             	(mandatory for add, optional for edit) the LDAP provider type of server used for the domain. Among the supported providers IPA,RHDS and ActiveDirectory.\n"
        printf "\tUSER   			 (optional for edit, mandatory for add) the domain user.\n"
        printf "\tPASSWORD_FILE   		 (optional for edit, mandatory for add) a file containing the password in the first line.\n"
	printf "\tinteractive        alternative for using -passwordFile - read the password interactively.\n"
        printf "\tPATH               (optional) use the given alternate configuration file.\n"
        printf "\tLDAP_SERVERS              (optional) a comma delimited list of LDAP servers to be set to the domain.\n"
        printf "\n"
        printf "\tAvailable actions:\n"
        printf "\tadd\n"
		printf "\tExamples:\n"
		printf "\t\t-action=add -domain=example.com -user=admin -provider=IPA -passwordFile=/tmp/.pwd\n"
		printf "\t\t\tAdd a domain called example.com, using user admin with ldap server type IPA and read the password from /tmp/.pwd.\n"
		printf "\t\t-action=edit -domain=example.com -provider=ActiveDirectory -passwordFile=/tmp/.new_password\n"
		printf "\t\t\tEdit the domain example.com, using another password file and updated the provider type to Active Directory.\n"
		printf "\t\t-action=delete -domain=example.com [-forceDelete]\n"
		printf "\t\t\tDelete the domain example.com.\n"
		printf "\t\t-forceDelete Optional parameter used in combination with -action=delete to skip confirmation of operation.\n"
		printf "\t\t\tDefault behaviour is prompt for confirmation of delete.\n"
		printf "\t\t-action=validate\n"
		printf "\t\t\tValidate the current configuration (go over all the domains, try to authenticate to each domain using the configured user/password.).\n"
		printf "\t\t-report In combination with -action=validate will report all validation error, if occured.\n"
		printf "\t\t\tDefault behaviour is to exit when a validation error occurs.\n"
		printf "\t\t-addPermissions In combination with -action=add/edit will add engine superuser permissions to the user.\n"
		printf "\t\t\tDefault behaviour is not to add permissions.\n"
		printf "\t\t-action=list\n"
		printf "\t\t\tLists the current configuration.\n"
		printf "\t\t-h\n"
		printf "\t\t\tShow this help.\n"

        return 0
}

if [ "$#" -gt 7 -o "$#" -lt 1 ]; then
        usage
		exit 1
fi


if [ "$1" == "--help" -o "$1" == "-h" ]; then
        usage
        exit 0
fi

PROPERTIES_FILE=`mktemp`

if [ ! -e $PROPERTIES_FILE ]; then
	die "Error: Temporary properties file cannot be created\n"
fi

cat << EOF > $PROPERTIES_FILE
AdUserName=
AdUserPassword.type=CompositePassword
LDAPSecurityAuthentication=
DomainName=
AdUserId=
LdapServers=
LDAPProviderTypes=
LDAPServerPort=
EOF

#
# Add this option to the java command line to enable remote debugging in
# all IP addresses and port 8787:
#
# -Xrunjdwp:transport=dt_socket,address=0.0.0.0:8787,server=y,suspend=y
#
# Note that the "suspend=y" options is needed to suspend the execution
# of the JVM till you connect with the debugger, otherwise it is
# not possible to debug the execution of the main method.
#

"${JAVA_HOME}/bin/java" \
  -Dlog4j.configuration="file:${ENGINE_ETC}/engine-manage-domains/log4j.xml" \
  -jar "${JBOSS_HOME}/jboss-modules.jar" \
  -dependencies org.ovirt.engine.core.tools \
  -class org.ovirt.engine.core.domains.ManageDomains \
  "$@" -propertiesFile=$PROPERTIES_FILE

RET_VAL=$?

rm $PROPERTIES_FILE

exit $RET_VAL

