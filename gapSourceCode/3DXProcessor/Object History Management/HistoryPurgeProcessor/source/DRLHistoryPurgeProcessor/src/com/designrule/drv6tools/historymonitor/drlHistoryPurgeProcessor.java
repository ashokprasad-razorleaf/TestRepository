package com.designrule.drv6tools.historymonitor;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.designrule.drv6tools.activities.drQueueObject;
import com.designrule.drv6tools.activities.drQueueObjectStatusEnum;
import com.designrule.drv6tools.common.drAbortProcessException;
import com.designrule.drv6tools.common.drApplicationException;
import com.designrule.drv6tools.common.drBusinessObject;
import com.designrule.drv6tools.jobserver.drJobServerCustomProcessor;
import com.designrule.drv6tools.jobserver.drJobServerEnoviaObject;

import matrix.db.BusinessObjectWithSelect;
import matrix.util.MatrixException;

public class drlHistoryPurgeProcessor extends drJobServerCustomProcessor {
	
	public drlHistoryPurgeProcessor(drJobServerEnoviaObject processorSystemEnoviaObject) {
		super(processorSystemEnoviaObject);
	}

	public drlHistoryPurgeProcessor(drJobServerEnoviaObject processorSystemEnoviaObject, String objectID)
			throws MatrixException {
		super(processorSystemEnoviaObject, objectID);
	}

	public drlHistoryPurgeProcessor(drJobServerEnoviaObject processorSystemEnoviaObject, String type, String name,
			String revision, String vault) throws MatrixException {
		super(processorSystemEnoviaObject, type, name, revision, vault);
	}

	public drlHistoryPurgeProcessor(drJobServerEnoviaObject processorSystemEnoviaObject, drBusinessObject drBusObj) {
		super(processorSystemEnoviaObject, drBusObj);
	}

	public drlHistoryPurgeProcessor(drJobServerEnoviaObject processorSystemEnoviaObject,
			BusinessObjectWithSelect businessObjectWithSelect) throws MatrixException, drApplicationException {
		super(processorSystemEnoviaObject, businessObjectWithSelect);
	}
	
	private String getDaysToPurgeObjects() throws drApplicationException {
		return super.getArg1();
	}
	
	@Override
    protected drQueueObjectStatusEnum process(drQueueObject queueObject) {
        String tempPath = "";
        try {
            log.debug("Entering History purge Script Processor");
            this.setQueueObject(queueObject);
            this.resetTempPath();
            tempPath = this.getTempPath();
     
            this.runHistoryPurgeProcess();

            return drQueueObjectStatusEnum.Processed;
        } catch (drAbortProcessException e) {
            log.error("Problem processing the file, process aborted ", e);
            return drQueueObjectStatusEnum.ProcessingFailed;
        } catch (Exception e) {
            log.error("Problem processing the file ", e);
            return drQueueObjectStatusEnum.ProcessingFailed;
        } catch (Throwable e) {
            log.error("Problem processing the file " + e.toString());
            return drQueueObjectStatusEnum.ProcessingFailed;
        } finally {
            this.deleteTempFiles(tempPath);
        }
    }
	
	protected void runHistoryPurgeProcess() throws drApplicationException {
        try {
            log.debug("=======================HistoryPurgeProcess=======================");
            
            //SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");	
            DateFormat sdf = this.drContext().getDateFormat();
			String xDays = this.getDaysToPurgeObjects();
			log.debug("Last days from which we need to delete ============= " + xDays);
			
			Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, -1);
            Date date = cal.getTime();
            String strDate = sdf.format(date);
            log.debug("Current date============================================= " + strDate);
           
            cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -Integer.parseInt(xDays));
            Date pastDate = cal.getTime();
            String strPastDate = sdf.format(pastDate);
            log.debug("Past date============================================= " + strPastDate);
           
            String typePattern = "DRLHistoryObject";
            String namePattern = "*";
            String revisionPattern = "*";
            String whereClause = "originated < '" + strPastDate + "' && attribute[DRL_HISTORY_Publish]==TRUE";
            //String whereClause = "originated < " + strPastDate;
            
            this.drContext().PushContext();
	    	this.drContext().startTransaction(true);
	    	ArrayList<String> listOfBusinessObjects = this.drContext().getListOfBusinessObjectIDs(typePattern, namePattern, revisionPattern, whereClause, 0);
			//drBusinessObjects listOfBusinessObjects = this.drContext().getListOfBusinessObjects(typePattern, namePattern, revisionPattern, whereClause);
			log.debug("Number of History Objects to be Purged:======================== " + listOfBusinessObjects.size());
			if(listOfBusinessObjects != null && listOfBusinessObjects.size() > 0) {
				for (String objectID : listOfBusinessObjects) {
					drBusinessObject drBoPurge = new drBusinessObject(this.drContext(), objectID);
					log.debug("Purged Object TNR:======================== " + drBoPurge.getTNR());
					drBoPurge.Delete();
				}
			}
			this.drContext().commitTransaction();            
            this.drContext().PopContext();
            
        } catch (Exception ex) {
            this.drContext().abortTransaction();
            throw new drApplicationException("Problem occurred at drlHistoryPurgeProcessor:runHistoryPurgeProcess : "+ex.getMessage(), ex);
        }
    }
	
}
