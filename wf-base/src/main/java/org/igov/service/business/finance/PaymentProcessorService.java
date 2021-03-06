package org.igov.service.business.finance;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import liquibase.util.csv.CSVReader;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.igov.io.GeneralConfig;
import org.igov.util.ToolLuna;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Service
public class PaymentProcessorService {
	
	private static final String VARIABLE_WITH_ORDER_ID = "PURPOSE";

	private final static Logger LOG = LoggerFactory.getLogger(PaymentProcessorService.class);
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService oTaskService;
    
    @Autowired
    private GeneralConfig generalConfig;
    
    public void loadPaymentInformation(){
    	List<Map<String, String>> paymentsList = loadPayments();
        
        for (Map<String, String> currPayment : paymentsList){
        	String orderID = loadOrderId(currPayment);
        	LOG.info("Loaded order ID {}", orderID);
        	
        	String[] as=orderID.split("\\-");
            Long nID_Protected = Long.valueOf(as[1]);
            
            Long nID_Process = ToolLuna.getOriginalNumber(nID_Protected);
            
            LOG.info("Original process ID {}", nID_Process);
        	List<Task> tasks = oTaskService.createTaskQuery().processInstanceId(nID_Process.toString()).active().list();
        	List<ProcessInstance> processes = runtimeService.createProcessInstanceQuery().processInstanceId(nID_Process.toString()).active().list();
        	LOG.info("Found {} process for the process ID {}", processes != null ? processes.size() : 0, nID_Process);
        	LOG.info("Found {} tasks for the process ID {}", tasks != null ? tasks.size() : 0, nID_Process);
        	for (Task task : tasks){
        		for (Map.Entry<String, String> currElem : currPayment.entrySet()){
        			oTaskService.setVariable(task.getId(), currElem.getKey(), currElem.getValue());
        		}
        		oTaskService.setVariable(task.getId(), "sID_Payment", currPayment.get("NUM"));
        		task.getTaskLocalVariables().put("sID_Payment", currPayment.get("NUM"));
        		LOG.info("Set variables {} to the task {}:{}", currPayment, task.getId(), task.getName());
        		LOG.info("Set variable sID_Payment:{} to the task {}", currPayment.get("NUM"), task.getId());
        	}
        	for (ProcessInstance processInstance : processes){
        		for (Map.Entry<String, String> currElem : currPayment.entrySet()){
        			runtimeService.setVariable(nID_Process.toString(), currElem.getKey(), currElem.getValue());
        		}
        		runtimeService.setVariable(nID_Process.toString(), "sID_Payment", currPayment.get("NUM"));
        		LOG.info("Set variables {} to the process instance {}", currPayment, processInstance.getId());
        		LOG.info("Set variable sID_Payment:{} to the process instance {}", currPayment.get("NUM"), processInstance.getId());
        	}
        }
    }
    
    private String loadOrderId(Map<String, String> currPayment) {
		if (currPayment.containsKey(VARIABLE_WITH_ORDER_ID)){
			String cutSring = StringUtils.substringAfter(currPayment.get(VARIABLE_WITH_ORDER_ID), "=");
			return StringUtils.substringBefore(cutSring, ";");
		}
		return null;
	}

	private List<Map<String, String>> loadPayments() {
		List<Map<String, String>> res = new LinkedList<Map<String, String>>();

		String fileName = loadFileFromServer();

		try {
			CSVReader reader = new CSVReader(new FileReader(fileName), ';');
			String[] headerArr = reader.readNext();
			LOG.info("Parsed header {}:{}", headerArr.toString(), headerArr.length);

			String[] nextLine = null;
			while ((nextLine = reader.readNext()) != null) {
				LOG.info("Parsing array {}:{}", nextLine.toString(), nextLine.length);
				Map<String, String> currElem = new HashMap<String, String>();
				for (int i = 0; i < nextLine.length; i++) {
					currElem.put(headerArr[i], nextLine[i]);
				}
				res.add(currElem);
			}
			reader.close();
		} catch (Exception e) {
			LOG.error("Exception occured while parsing csv file {}",
					e.getMessage(), e);
		}

		File file = new File(fileName);

		file.delete();

		return res;
	}
	
	private String loadFileFromServer(){
		JSch jsch = new JSch();
        Session session = null;
        File file = null;
        try {
        	LOG.info("Loading file from the server {}:{} with parameters. Login:{} password:{} fileName:{}", 
        			generalConfig.getPayYuzhnyFTPsHost(), generalConfig.getPayYuzhnyFTPnPort(), 
        			generalConfig.getPayYuzhnyFTPsUser(), generalConfig.getPayYuzhnyFTPsPassword(), 
        			generalConfig.getPayYuzhnyFTPsFileName());
        	file = File.createTempFile("11082016", ".csv");
            session = jsch.getSession(generalConfig.getPayYuzhnyFTPsUser(), generalConfig.getPayYuzhnyFTPsHost(), Integer.valueOf(generalConfig.getPayYuzhnyFTPnPort()));
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(generalConfig.getPayYuzhnyFTPsPassword());
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.get(generalConfig.getPayYuzhnyFTPsFileName(), file.getAbsolutePath());
            sftpChannel.exit();
            session.disconnect();
        } catch (JSchException e) {
        	LOG.error("Exception occured while getting file from sftp {}", e.getMessage(), e);  
        } catch (SftpException e) {
        	LOG.error("Exception occured while getting file from sftp {}", e.getMessage(), e);  
        } catch (IOException e) {
        	LOG.error("Exception occured while getting file from sftp {}", e.getMessage(), e);  
		}
        return file != null ? file.getAbsolutePath() : null;
	}
}
