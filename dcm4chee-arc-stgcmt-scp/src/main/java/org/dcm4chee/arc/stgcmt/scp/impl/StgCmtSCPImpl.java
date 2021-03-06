package org.dcm4chee.arc.stgcmt.scp.impl;

import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.IApplicationEntityCache;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.pdu.RoleSelection;
import org.dcm4che3.net.service.AbstractDicomService;
import org.dcm4che3.net.service.DicomService;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4chee.arc.entity.QueueMessage;
import org.dcm4chee.arc.qmgt.Outcome;
import org.dcm4chee.arc.qmgt.QueueManager;
import org.dcm4chee.arc.stgcmt.scp.StgCmtSCP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.jms.ObjectMessage;
import java.io.IOException;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since Sep 2015
 */
@ApplicationScoped
@Typed({ DicomService.class, StgCmtSCP.class })
class StgCmtSCPImpl extends AbstractDicomService implements StgCmtSCP {

    private static final Logger LOG = LoggerFactory.getLogger(StgCmtSCPImpl.class);

    @Inject
    private QueueManager queueManager;

    @Inject
    private Device device;

    @Inject
    private IApplicationEntityCache aeCache;


    public StgCmtSCPImpl() {
        super(UID.StorageCommitmentPushModelSOPClass);
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                          Attributes rq, Attributes actionInfo) throws IOException {
        if (dimse != Dimse.N_ACTION_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);

        int actionTypeID = rq.getInt(Tag.ActionTypeID, 0);
        if (actionTypeID != 1)
            throw new DicomServiceException(Status.NoSuchActionType)
                    .setActionTypeID(actionTypeID);

        String localAET = as.getLocalAET();
        String remoteAET = as.getRemoteAET();
        try {
            as.getApplicationEntity().findCompatibelConnection(aeCache.findApplicationEntity(remoteAET));
            scheduleStorageCommitment(localAET, remoteAET, actionInfo);
        } catch (ConfigurationNotFoundException e) {
            throw new DicomServiceException(Status.ProcessingFailure, "Unknown Calling AET: " + remoteAET);
        } catch (Exception e) {
            throw new DicomServiceException(Status.ProcessingFailure, e.getMessage());
        }
        try {
            as.writeDimseRSP(pc, Commands.mkNActionRSP(rq, Status.Success), null);
        } catch (Exception e) {
            LOG.warn("{} << N-ACTION-RSP failed: {}", as, e.getMessage());
        }
    }

    private void scheduleStorageCommitment(String localAET, String remoteAET, Attributes actionInfo)
            throws Exception {
        ObjectMessage msg = queueManager.createObjectMessage(actionInfo);
        msg.setStringProperty("LocalAET", localAET);
        msg.setStringProperty("RemoteAET", remoteAET);
        queueManager.scheduleMessage(QUEUE_NAME, msg);
    }

    @Override
    public Outcome sendNEventReport(String localAET, String remoteAET, Attributes eventInfo)
            throws Exception  {
            ApplicationEntity localAE = device.getApplicationEntity(localAET, true);
            ApplicationEntity remoteAE = aeCache.findApplicationEntity(remoteAET);
            AAssociateRQ aarq = mkAAssociateRQ(localAE, localAET);
            Association as = localAE.connect(remoteAE, aarq);
            try {
                int successful = sequenceSizeOf(eventInfo, Tag.ReferencedSOPSequence);
                int failed = sequenceSizeOf(eventInfo, Tag.FailedSOPSequence);
                DimseRSP neventReport = as.neventReport(
                        UID.StorageCommitmentPushModelSOPClass,
                        UID.StorageCommitmentPushModelSOPInstance,
                        failed > 0 ? 2 : 1, eventInfo, null);
                neventReport.next();
                return new Outcome(failed > 0 ? QueueMessage.Status.WARNING : QueueMessage.Status.COMPLETED,
                        "Return Storage Commitment Result[successful: " + successful + ", failed: " + failed
                                + "] to AE: " + remoteAET);
            } finally {
                try {
                    as.release();
                } catch (IOException e) {
                    LOG.info("{}: Failed to release association to {}", as, remoteAET);
                }
            }
    }

    private int sequenceSizeOf(Attributes eventInfo, int seqTag) {
        Sequence seq = eventInfo.getSequence(seqTag);
        return seq != null ? seq.size() : 0;
    }

    private AAssociateRQ mkAAssociateRQ(ApplicationEntity localAE, String localAET) {
        AAssociateRQ aarq = new AAssociateRQ();
        aarq.setCallingAET(localAET);
        TransferCapability tc = localAE.getTransferCapabilityFor(UID.StorageCommitmentPushModelSOPClass,
                TransferCapability.Role.SCP);
        aarq.addPresentationContext(new PresentationContext(1, UID.StorageCommitmentPushModelSOPClass,
                tc.getTransferSyntaxes()));
        aarq.addRoleSelection(new RoleSelection(UID.StorageCommitmentPushModelSOPClass, false, true));
        return aarq;
    }
}
