package org.jboss.resteasy.test.resource.basic.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/postoffice")
public class PostOfficeResource {

    public static final String UPLOAD_FOLDER = "/tmp/z";

    @Context
    private ServletContext context;

    @GET
    @Path("/ping")
    @Produces("text/plain")
    public String ping() {
        return "postoffice-pong";
    }

    @PUT
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(MultipartFormDataInput input) throws IOException {

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();

        // Get file data to save
        List<InputPart> inputParts = uploadForm.get("attachment");

        for (InputPart inputPart : inputParts) {
            try {
                MultivaluedMap<String, String> header = inputPart.getHeaders();
                String fileName = getFileName(header);

                // convert the uploaded file to inputstream
                InputStream inputStream = inputPart.getBody(InputStream.class, null);
                File customDir = new File(UPLOAD_FOLDER);
                if (!customDir.exists()) {
                    customDir.mkdir();
                }
                fileName = customDir.getCanonicalPath() + File.separator + fileName;

                try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                    inputStream.transferTo(outputStream);
                }

                return Response.status(200).entity("Uploaded file name : " + fileName
                        + " . <br/> <a href='" + context.getContextPath() + "'>Back</a>").build();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String getFileName(MultivaluedMap<String, String> header) {

        String[] contentDisposition = header.getFirst("Content-Disposition")
                .split(";");

        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }
}
