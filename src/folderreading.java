import java.io.File;

import java.io.FileInputStream;

import java.io.FileNotFoundException;

import java.io.InputStream;

import com.filenet.api.core.ObjectStore;

import com.filenet.api.collection.ContentElementList;



import com.filenet.api.constants.AutoClassify;



import com.filenet.api.constants.AutoUniqueName;



import com.filenet.api.constants.CheckinType;



import com.filenet.api.constants.RefreshMode;



import com.filenet.api.core.ContentTransfer;



import com.filenet.api.core.Document;



import com.filenet.api.core.Factory;



import com.filenet.api.core.Folder;

import com.filenet.api.core.ReferentialContainmentRelationship;



import com.filenet.api.util.Id;



import com.ibm.casemgmt.api.Case;



import com.ibm.casemgmt.api.CaseType;



import com.ibm.casemgmt.api.constants.ModificationIntent;



import com.ibm.casemgmt.api.objectref.ObjectStoreReference;



import org.apache.commons.json.JSONObject;

public class folderreading {

	public static void main(String args[]) throws FileNotFoundException{

	 File file = new File(



	            "C:\\Attachments\\Mortgage-Contract-Signed.pdf");

	 InputStream is=new FileInputStream(file);

	 String fileName=file.getName();

	 createCaseWithDocument(is,fileName);

}

	public static String createCaseWithDocument(InputStream is,String fileName) {



		String METHOD_NAME = "createCaseWithDocument";



		System.out.println(METHOD_NAME);



		



		try {



			



			ObjectStore targetOs = new ConnectingCase().getConnection();



			ObjectStoreReference targetOsRef = new ObjectStoreReference(targetOs);



			CaseType caseType = CaseType.fetchInstance(targetOsRef, "CM_Contract_Management_CT");



			Case pendingCase = Case.createPendingInstance(caseType);

			pendingCase.getProperties().putObjectValue("CM_Source", "SharedPath");

			pendingCase.save(RefreshMode.REFRESH, null, ModificationIntent.MODIFY);



			String caseId = pendingCase.getId().toString();



			Document document = Factory.Document.createInstance(targetOs, "CM_ContractDocument");



			ContentElementList contentList = Factory.ContentElement.createList();



			ContentTransfer content = Factory.ContentTransfer.createInstance();



			content.setCaptureSource(is);

			content.set_ContentType("application/pdf");
			content.set_RetrievalName(fileName);

			contentList.add(content);



			document.set_ContentElements(contentList);



			document.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);



			com.filenet.api.property.Properties properties = document.getProperties();



			((com.filenet.api.property.Properties) properties).putValue("DocumentTitle", fileName);



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