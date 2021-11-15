import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import storageSpec.AbstractUser;
import storageSpec.Storage;
import storageSpec.UserManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TestClass {//delete this class later
    public static void main(String... args) throws IOException, GeneralSecurityException {
        try {
            Class.forName("GoogleDriveUser");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        AbstractUser user = UserManager.getUser("ntadic4419rn","myPassword", null);
        //user.initStorage("moje novo sk", "google drive");
//        Storage s = new Storage("bla", user, "rootLocation", "id");
//        user.setCurrentActiveStorage(s);
//            ArrayList<String> a = (ArrayList<String>) user.getFilesInDirSortedByName("bla");
//            for(String s: a){
//                System.out.println(s);
//            }
//            List<String> list = new ArrayList<>();
//            list.add("www");
//            list.add("Resenja.zip");
//            user.move(list, "bla");


           // user.move("bla/dasda/aaa.txt", "bla/dasda");
        //user.download("bla", "C:\\Users\\tadic\\Downloads");
           // user.searchByName("lolololo");
           // user.initStorage("storage!", "drive");
            //user.createFile("fileName222", "storage!", "C:\\Users\\tadic\\Desktop", "/txt");
            //user.createDir("myNewDir", "storage!", "fajl br", 5);

        //user.download("bla", "C:\\Users\\tadic\\Downloads");
//          DateTime dt = (DateTime) user.getCreationDate("sss");

        //System.out.println(dt.toString());
//          ArrayList<String> a = new ArrayList<>();
//          user.initStorage("ms", "drive");
//          user.uploadExistingFile("3b", "ms", "C:\\Users\\tadic\\Desktop\\3b.txt","/txt" );

//          a = (ArrayList<String>) user.searchByExtension("application/vnd.google-apps.folder");
//            for(String s: a){
//                System.out.println(s);
//            }
//          user.initStorage("mojeSkladiste", "drive");
//          user.createDir("dir1", "mojeSkladiste");
//          user.uploadExistingFile("3b", "mojeSkladiste/dir1", "C:\\Users\\tadic\\Desktop\\3b.txt","/txt" );
//          user.download("mojeSkladiste", "C:\\Users\\tadic\\Downloads");

//        user.delete("bzvz");
//        user.setUserName("ntadic");
//        user.setPassword("mypass");
//        user.initStorage("skladisteTest", "drive");
//

//
//        user.createDir("dir1", "skladisteTest");
//        user.createDir("dir2", "skladisteTest/dir1");
//        user.createDir("dir3", "skladisteTest");
//        user.createDir("dir4", "skladisteTest/dir1/dir2");
//        user.uploadExistingFile("mojNoviFajl","skladisteTest/dir1/dir2/dir4" ,"C:\\Users\\tadic\\Desktop\\aaa.txt","/txt");

    }
}
