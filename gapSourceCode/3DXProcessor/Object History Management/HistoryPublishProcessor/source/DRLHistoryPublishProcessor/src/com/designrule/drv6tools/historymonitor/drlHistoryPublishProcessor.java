package com.designrule.drv6tools.historymonitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.designrule.drv6tools.activities.drQueueObject;
import com.designrule.drv6tools.activities.drQueueObjectStatusEnum;
import com.designrule.drv6tools.common.drAbortProcessException;
import com.designrule.drv6tools.common.drApplicationException;
import com.designrule.drv6tools.common.drBusinessObject;
import com.designrule.drv6tools.jobserver.drJobServerCustomProcessor;
import com.designrule.drv6tools.jobserver.drJobServerEnoviaObject;

import matrix.db.BusinessObjectWithSelect;
import matrix.util.MatrixException;

public class drlHistoryPublishProcessor extends drJobServerCustomProcessor {

	public drlHistoryPublishProcessor(drJobServerEnoviaObject processorSystemEnoviaObject) {
		super(processorSystemEnoviaObject);
	}

	public drlHistoryPublishProcessor(drJobServerEnoviaObject processorSystemEnoviaObject, String objectID)
			throws MatrixException {
		super(processorSystemEnoviaObject, objectID);
	}

	public drlHistoryPublishProcessor(drJobServerEnoviaObject processorSystemEnoviaObject, String type, String name,
			String revision, String vault) throws MatrixException {
		super(processorSystemEnoviaObject, type, name, revision, vault);
	}

	public drlHistoryPublishProcessor(drJobServerEnoviaObject processorSystemEnoviaObject, drBusinessObject drBusObj) {
		super(processorSystemEnoviaObject, drBusObj);
	}

	public drlHistoryPublishProcessor(drJobServerEnoviaObject processorSystemEnoviaObject,
			BusinessObjectWithSelect businessObjectWithSelect) throws MatrixException, drApplicationException {
		super(processorSystemEnoviaObject, businessObjectWithSelect);
	}
	
	private String getPublishFormat() throws drApplicationException {
		return super.getArg1();
	}
	
	private String getDBConnectionString() throws drApplicationException {
		return super.getArg2();
	}
	
	private String getDBTableName() throws drApplicationException {
		return super.getArg3();
	}
		
	private String getExportPath() throws drApplicationException {
		return super.getMultilineArg2();
	}
			
	@Override
    protected drQueueObjectStatusEnum process(drQueueObject queueObject) {
        String tempPath = "";
        try {
            log.debug("Entering History Publish Script Processor");
            this.setQueueObject(queueObject);
            this.resetTempPath();
            tempPath = this.getTempPath();
     
            this.runHistoryPublishProcess();

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
	
	protected void runHistoryPublishProcess() throws drApplicationException {
        try {
            log.debug("=======================HistoryPublishProcess=======================");
                                    
            String typePattern = "DRLHistoryObject";
            String namePattern = "*";
            String revisionPattern = "*";
            String whereClause = "attribute[DRL_HISTORY_Publish]==FALSE";
            
            this.drContext().PushContext();
	    	this.drContext().startTransaction(true);
			//drBusinessObjects listOfBusinessObjects = this.drContext().getListOfBusinessObjects(typePattern, namePattern, revisionPattern, whereClause);
			ArrayList<String> listOfBusinessObjects = this.drContext().getListOfBusinessObjectIDs(typePattern, namePattern, revisionPattern, whereClause, 0);
			log.debug("Number of History Objects to be Published:======================== " + listOfBusinessObjects.size());
			if(listOfBusinessObjects != null && listOfBusinessObjects.size() > 0) {
				for (String objectID : listOfBusinessObjects) {
					drBusinessObject drBOPublish = new drBusinessObject(this.drContext(), objectID);
					log.debug("Published Object TNR:======================== " + drBOPublish.getTNR());
					publishData(drBOPublish);
				}
			}
			this.drContext().commitTransaction();            
            this.drContext().PopContext();
            
        } catch (Exception ex) {
            this.drContext().abortTransaction();
            throw new drApplicationException("Problem occurred at drlHistoryPulishProcessor:runHistoryPublishProcess : "+ex.getMessage(), ex);
        }
    }
	
	protected void publishData(drBusinessObject drBOPublish) throws Exception {
		String sPublishFormat = this.getPublishFormat();
		switch(sPublishFormat) {
			case "DB" :
				publishViaDB(drBOPublish);
				break;
			case "CSV" :
				publishViaCSV(drBOPublish);
				break;
			case "XML" :
				publishViaXML(drBOPublish);
				break;
		}
	}
	

	@SuppressWarnings("rawtypes")
	protected void publishViaDB(drBusinessObject drBOPublish) throws drApplicationException, SQLException, MatrixException {
		String sTableName = this.getDBTableName();
		String dbURL = (String) this.getDBConnectionString().trim();
        
        HashMap<String, String> attributeDBMap = this.getValueAsHashMap(ATTRIBUTE_NAME_DRL_MULTILINE_ARG_1);
        Connection conn = DriverManager.getConnection(dbURL);        
        if (conn != null) {
			HashMap<String, String> mappedMap = (HashMap<String, String>) getMappedData(attributeDBMap, getObjectInfoMap(drBOPublish));
			StringBuilder sbColumnName = new StringBuilder();
			StringBuilder sbColumnValue = new StringBuilder();
			Iterator it = mappedMap.entrySet().iterator();
		    while (it.hasNext()) {
				Map.Entry map = (Map.Entry)it.next();
		        sbColumnName.append(map.getKey()).append(",");
		        sbColumnValue.append("'").append(map.getValue()).append("'").append(",");
		        it.remove();
		    }
		    
		    String sColumnName = sbColumnName.toString().trim();
		    String sColumnValue = sbColumnValue.toString().trim();
		    
		    StringBuilder sbSQLStatement = new StringBuilder();
		    sbSQLStatement.append("INSERT INTO ").append(sTableName).append(" (").append(sColumnName.substring(0, sColumnName.length() - 1));		   
		    sbSQLStatement.append(") VALUES (").append(sColumnValue.substring(0, sColumnValue.length() - 1)).append(")");
		    
		    Statement insertStatement = conn.createStatement();
		    insertStatement.execute(sbSQLStatement.toString());
		    
		    drBOPublish.setValue("DRL_HISTORY_Publish", "TRUE");
		    drBOPublish.save();
			//this.drContext().getResultFromMQLCommand("mod bus "+drBOPublish.getObjectID()+" DRL_HISTORY_Publish TRUE");
        }
	}
		
	protected HashMap<String, String> getObjectInfoMap(drBusinessObject drBOPublish) throws drApplicationException, MatrixException {		
		
		ArrayList<String> selectables = new ArrayList<String>();
    	selectables.add("attribute[DRL_HISTORY_Event]");
    	selectables.add("attribute[DRL_HISTORY_Site]");
    	selectables.add("name");
    	selectables.add("attribute[DRL_HISTORY_ObjectId]");
    	selectables.add("attribute[DRL_HISTORY_Type]");
    	selectables.add("attribute[DRL_HISTORY_ObjectDisplayName]");
    	selectables.add("originated");
    	HashMap<String, String> objectInfoMap = drBOPublish.getInfo(selectables);
		
		return objectInfoMap;
	}
	
	protected HashMap<String, String> getMappedData(HashMap<String, String> attributeDBMap, HashMap<String, String> objMap) throws drApplicationException {
		HashMap<String, String> mappedMap = new HashMap<String, String>();
		for (Map.Entry<String, String> entry1 : attributeDBMap.entrySet()) {
			  String value1 = entry1.getKey();
			  String key = entry1.getValue();
			  String value2 = objMap.get(key);
			  mappedMap.put(value1, value2);
		}
		return mappedMap;
	}

	@SuppressWarnings("unused")
	protected void publishViaCSV(drBusinessObject drBOPublish) throws Exception {
		String sPath = (String) this.getExportPath();
        //LOGIC to Export Map to CSV
	}

	@SuppressWarnings("unused")
	protected void publishViaXML(drBusinessObject drBOPublish) throws Exception {
		String sPath = (String) this.getExportPath();
		//LOGIC to Export Map to XML
	}
	
}
