package com.atlas.webservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.dassault_systemes.platform.restServices.RestService;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.json.JSONObject;

import matrix.db.Context;
import matrix.db.JPO;


public class gapWebService extends RestService {

	@GET
	@Path("/getFileView")
	@Produces({ MediaType.APPLICATION_JSON })
	public void getFileView(@javax.ws.rs.core.Context HttpServletRequest request, @QueryParam("type") String type,
			@QueryParam("name") String name, @QueryParam("revision") String revision,
			@javax.ws.rs.core.Context HttpServletResponse response) throws Exception {
		Context context = null;
		try {
			context = getAuthenticatedContext(request, false);
			DownloadFile(context, request, response, type, name, revision);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		} finally {
			if (context != null && context.isConnected()) {
				context.disconnect();
			}
		}

	}

	@GET
	@Path("/getFileViews")
	@Produces({ MediaType.TEXT_HTML })
	public void getFileViews(@javax.ws.rs.core.Context HttpServletRequest request, @QueryParam("type") String type,
			@QueryParam("name") String name, @QueryParam("revision") String revision,
			@QueryParam("username") String username, @QueryParam("password") String password,
			@javax.ws.rs.core.Context HttpServletResponse response) throws Exception {
		Context context = null;
		try {
			String serverURL =FrameworkProperties.getProperty("emxFramework.Webservice.URL");
			context = getContext(request, response, serverURL, username, password);
			if (context.isConnected()) {
				DownloadFile(context, request, response, type, name, revision);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		} finally {
			if (context != null && context.isConnected()) {
				context.disconnect();
			}
		}
	}

	// Method to get the context of user with given credentials
	public matrix.db.Context getContext(HttpServletRequest req, HttpServletResponse resp, String serverURL, String user,
			String password) throws Exception {
		matrix.db.Context eMatrixContext = null;
		try {
			eMatrixContext = new matrix.db.Context(serverURL);
			eMatrixContext.setUser(user);
			eMatrixContext.setPassword(password);
			eMatrixContext.connect();

		} catch (Exception ex) {
			resp.sendError(HttpURLConnection.HTTP_UNAUTHORIZED, ex.getMessage());
		}
		return eMatrixContext;
	}

	// Method converts the Server Files to Binary stream and writes output stream
	// onto client browser.
	public void DownloadFile(Context context, HttpServletRequest request, HttpServletResponse response, String type,
			String name, String revision) throws Exception {
		String strWorkSpacePath = null;
		try {
			HashMap fields = new HashMap();
			fields.put("type", type);
			fields.put("name", name);
			fields.put("revision", revision);

			String[] methodArgs = JPO.packArgs(fields);

			JSONObject jobDocFileDetails = JPO.invoke(context, "gapWebserviceJPO", null, "getDocumentFilesToCheckout",
					methodArgs, JSONObject.class);

			String strFile = jobDocFileDetails.getString("filename");
			String strResult = jobDocFileDetails.getString("result");
			String strMessage = jobDocFileDetails.getString("message");
			if (strResult.equals("SUCCESS") && !strFile.isEmpty()) {
				strWorkSpacePath = jobDocFileDetails.getString("workspacefolder");
				File downloadFile = new File(strFile);
				FileInputStream inStream = new FileInputStream(downloadFile);

				// obtains ServletContext
				ServletContext servletContext = request.getServletContext();

				// gets MIME type of the file
				String mimeType = servletContext.getMimeType(strFile);
				if (mimeType == null) {
					// set to binary type if MIME mapping not found
					mimeType = "application/octet-stream";
				}

				// modifies response
				response.setContentType(mimeType);
				response.setContentLength((int) downloadFile.length());

				// forces download
				String headerKey = "Content-Disposition";
				String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
				response.setHeader(headerKey, headerValue);
				// obtains response's output stream
				OutputStream outStream = response.getOutputStream();

				byte[] buffer = new byte[4096];
				int bytesRead = -1;

				while ((bytesRead = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}
				inStream.close();
				outStream.close();
			} else {
				response.sendError(HttpURLConnection.HTTP_NOT_FOUND, "The requested object [" + name + "] not found");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (strWorkSpacePath != null) {
				context.deleteWorkspace();
			}
		}
	}
}
