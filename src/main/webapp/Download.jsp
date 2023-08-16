<%@page import="jjdm.zocalo.DownloadHelper" %>
<%@page trimDirectiveWhitespaces="true" %>

<%

String fileName = request.getParameter("file");
String baseFileName = fileName.replaceAll(".log", "").replaceAll(" ", "_");
String type = request.getParameter("type");
String output = DownloadHelper.downloadLogFile(fileName, type);
String contentType = "text/plain";
String extension = "txt";

if(type.equals("csv")) {
	contentType = "text/csv";
	extension = "csv";
}

response.setContentType(contentType);
response.setHeader("Content-Disposition", "attachment; filename=" + baseFileName + "." + extension);
out.println(output);

%>