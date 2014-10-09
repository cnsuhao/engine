package org.ovirt.engine.core.common.businessentities.network;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.compat.Guid;

/**
 * <code>NetworkStatistics</code>
 *
 */
public abstract class NetworkStatistics implements BusinessEntity<Guid> {
    private static final long serialVersionUID = -748737255583275169L;

    private Guid id;

    private InterfaceStatus status;

    private Double receiveDropRate;

    private Double receiveRate;

    private Double transmitDropRate;

    private Double transmitRate;

    /**
     * Sets the instance id.
     *
     * @param id
     *            the id
     */
    public void setId(Guid id) {
        this.id = id;
    }

    /**
     * Returns the instance id.
     *
     * @return the id.
     */
    public Guid getId() {
        return id;
    }

    /**
     * Sets the status for the connection.
     *
     * @param status
     *            the status
     */
    public void setStatus(InterfaceStatus status) {
        this.status = status;
    }

    /**
     * Returns the connection status.
     *
     * @return the status
     */
    public InterfaceStatus getStatus() {
        return status;
    }

    /**
     * Sets the received data drop rate.
     *
     * @param receiveDropRate
     *            the rate
     */
    public void setReceiveDropRate(Double receiveDropRate) {
        this.receiveDropRate = receiveDropRate;
    }

    /**
     * Returns the received data drop rate.
     *
     * @return the rate
     */
    public Double getReceiveDropRate() {
        return receiveDropRate;
    }

    /**
     * Sets the data receive rate.
     *
     * @param receiveRate
     *            the rate
     */
    public void setReceiveRate(Double receiveRate) {
        this.receiveRate = receiveRate;
    }

    /**
     * Returns the data receive rate.
     *
     * @return the rate
     */
    public Double getReceiveRate() {
        return receiveRate;
    }

    /**
     * Sets the transmitted data drop rate.
     *
     * @param transmitDropRate
     *            the rate
     */
    public void setTransmitDropRate(Double transmitDropRate) {
        this.transmitDropRate = transmitDropRate;
    }

    /**
     * Returns the transmitted data drop rate.
     *
     * @return the rate
     */
    public Double getTransmitDropRate() {
        return transmitDropRate;
    }

    /**
     * Sets the data transmit rate.
     *
     * @param transmitRate
     *            the rate
     */
    public void setTransmitRate(Double transmitRate) {
        this.transmitRate = transmitRate;
    }

    /**
     * Returns the data transmit rate.
     *
     * @return the rate
     */
    public Double getTransmitRate() {
        return transmitRate;
    }
}
