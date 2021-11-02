



import com.filenet.api.collection.ContentElementList;

import com.filenet.api.constants.AutoClassify;

import com.filenet.api.constants.AutoUniqueName;

import com.filenet.api.constants.CheckinType;

import com.filenet.api.constants.RefreshMode;

import com.filenet.api.core.ContentTransfer;

import com.filenet.api.core.Document;

import com.filenet.api.core.Factory;

import com.filenet.api.core.Folder;

import com.filenet.api.core.ObjectStore;

import com.filenet.api.core.ReferentialContainmentRelationship;

import com.filenet.api.util.Id;

import com.ibm.casemgmt.api.Case;

import com.ibm.casemgmt.api.CaseType;

import com.ibm.casemgmt.api.constants.ModificationIntent;

import com.ibm.casemgmt.api.objectref.ObjectStoreReference;

import org.apache.commons.json.JSONObject;

import java.io.ByteArrayInputStream;

import java.io.InputStream;

import java.util.*;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.FlagTerm;


public class ThunderbirdFetch {
	public static void main( String[] args ) throws Exception {

	    Session session = Session.getDefaultInstance(new Properties( ));

	    Store store = session.getStore("imap");

	    store.connect("localhost",143,"default@ibmdba.com", "passw0rd");

	    javax.mail.Folder inbox = store.getFolder( "INBOX" );

	    inbox.open( javax.mail.Folder.READ_WRITE );

	    Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
           
	    for ( Message message : messages ) {
             message.setFlag(Flag.SEEN, true);
	        Address[] fromAddress = message.getFrom();

	        String from = fromAddress[0].toString();

	        String subject = message.getSubject();

	        String sentDate = message.getSentDate().toString();

	        String contentType = message.getContentType();

	        String messageContent = "";

	        String attachFiles = "";
	        String attachname=message.getFileName();

	        InputStream is = null;

	        if (contentType.contains("multipart")) {

	            Multipart multiPart = (Multipart) message.getContent();

	            int numberOfParts = multiPart.getCount();

	            for (int partCount = 0; partCount < numberOfParts; partCount++) {

	                MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);

	                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {

	                	String fileName = part.getFileName();

	                    attachFiles += fileName + ", ";

	                    is=part.getInputStream();

	                } else {

	                    messageContent = part.getContent().toString();

	                }

	            }

	            if (attachFiles.length() > 1) {

	                attachFiles = attachFiles.substring(0, attachFiles.length() - 2);

	            }

	        } else if (contentType.contains("text/plain")|| contentType.contains("text/html")) {

	                Object content = message.getContent();

	      if (content != null) {

	                messageContent = content.toString();

	            }

	        }

	        String rep[] = null,rep1[] = null,rep2[]=null,rep3[]=null;
            String conName="",comName="",amt="",catr="";
            double d=0;
	       if(messageContent.trim().length()>0){

	        	messageContent=messageContent.trim();
             // System.out.println(messageContent);
              rep=messageContent.split(":");
             // System.out.println(rep[1]+"second"+rep[2]+"third"+rep[3]+"fourth"+rep[4]);
              catr=rep[4];
              rep1=rep[3].split("Category");
              amt=rep1[0];
              rep2=rep[2].split("Amount");
              comName=rep2[0];
              rep3=rep[1].split("CompanyName");
              conName=rep3[0];
              System.out.println("catr"+catr+"amt"+amt+"comName"+comName+"conName"+conName);
              d=Double.parseDouble(amt);  
	         /* rep=messageContent.split(":");

	          rep1=rep[1].split("CompanyName");

	          System.out.println("first"+rep1[0].trim());

	          System.out.println("second"+rep[2]);
             System.out.println("rep"+rep[1]);*/
	        }

	        System.out.println("\t Attachments: " + attachFiles);

	        createCaseWithDocument(is, conName, comName,attachFiles,d,catr);

	    }

	   inbox.close(false);

	    store.close();

	  }

	  public static String createCaseWithDocument(InputStream is, String contractName, String companyName,String attachname,double amountUSD,String category) {

			String METHOD_NAME = "createCaseWithDocument";

			System.out.println(METHOD_NAME);

			

			try {

				

				ObjectStore targetOs = new ConnectingCase().getConnection();

				ObjectStoreReference targetOsRef = new ObjectStoreReference(targetOs);

				CaseType caseType = CaseType.fetchInstance(targetOsRef, "CM_Contract_Management_CT");

				Case pendingCase = Case.createPendingInstance(caseType);

				pendingCase.getProperties().putObjectValue("CM_ContractName", contractName);

				pendingCase.getProperties().putObjectValue("CM_CompanyName", companyName);
				pendingCase.getProperties().putObjectValue("CM_AmountUSD", amountUSD);
				pendingCase.getProperties().putObjectValue("CM_Category", category);
				pendingCase.getProperties().putObjectValue("CM_Source", "Email");
				pendingCase.save(RefreshMode.REFRESH, null, ModificationIntent.MODIFY);

				String caseId = pendingCase.getId().toString();

				Document document = Factory.Document.createInstance(targetOs, "Document");

				ContentElementList contentList = Factory.ContentElement.createList();

				ContentTransfer content = Factory.ContentTransfer.createInstance();

				content.setCaptureSource(is);
				content.set_ContentType("application/pdf");
				content.set_RetrievalName(attachname);
				contentList.add(content);

				document.set_ContentElements(contentList);

				document.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);

				com.filenet.api.property.Properties properties = document.getProperties();

				((com.filenet.api.property.Properties) properties).putValue("DocumentTitle", attachname);

				document.set_MimeType("application/pdf");

				document.save(RefreshMode.REFRESH);

				System.out.println("Filing PDF file to : " + caseId);

				

				Folder folder = Factory.Folder.fetchInstance(targetOs, new Id(caseId), null);

				//Folder folder = Factory.Folder.fetchInstance(objectStore, folderPath, null);

				ReferentialContainmentRelationship ref = folder.file(document, AutoUniqueName.AUTO_UNIQUE, "Testing", null);

				ref.save(RefreshMode.REFRESH);

				System.out.println("PDF filed in CaseFolder.");

				JSONObject jsonObject = new JSONObject();

				

				System.out.println("Path Extension: "+folder.get_Name()+"\t"+folder.get_DateCreated());

				String pathWithDate = folder.get_FolderName();

				System.out.println(pathWithDate);

				

				String docID=document.get_Id().toString();



				jsonObject.put("DocumentId", docID);

				jsonObject.put("CaseId",caseId);

				System.out.println("ExecutableClass.fileToCaseFolder()"+jsonObject.get("DocumentId")+"\t"+jsonObject.get("CaseId"));

				return jsonObject.toString();

			} catch (Exception e) {

				System.out.println(e.getMessage());

				System.out.println("ExecutableClass.fileToCaseFolder().Exception in case fodler");

				return null;

			}

		}
}
