import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import storageSpec.AbstractUser;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TestClass {//delete this class later
    public static void main(String... args) throws IOException, GeneralSecurityException {
//        AbstractUser user = new GoogleDriveUser();
//        user.setUserName("ntadic");
//        user.setPassword("mypass");
//        user.initStorage("novoskladisteee", "drive");
//        user.uploadExistingFile("mojNoviFajl", "C:\\Users\\tadic\\Desktop\\aaa.txt", "novoskladisteee", "/txt");
//        user.createFile("fileName", "C:\\Users\\tadic\\Desktop\\probaaaamooo.txt", "novoskladisteee", "/txt");
//        user.createDir("nestedDir", "novoskladisteee");
        /*
        Primer za listanje fajlova
        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }
         */
        //"novoskladisteee/nestedDir"
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        String pageToken = null;
        Drive driveService;
        driveService = new Drive.Builder(HTTP_TRANSPORT, DriveQuickstart.getJsonFactory(), DriveQuickstart.getCredentials(HTTP_TRANSPORT))
                .setApplicationName(DriveQuickstart.getApplicationName())
                .build();
        do {
            FileList result = driveService.files().list()
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            for (File file : result.getFiles()) {
                if(file.getName().equalsIgnoreCase("novoskladisteee")){
                    //file.get
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
    }
}
