import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import storageSpec.AbstractUser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;

public class TestClass {//delete this class later
    public static void main(String... args) throws IOException, GeneralSecurityException {
          AbstractUser user = new GoogleDriveUser();
          user.download("www", "C:\\Users\\tadic\\Downloads");
//        user.delete("bzvz");
//        user.setUserName("ntadic");
//        user.setPassword("mypass");
//        user.initStorage("skladisteTest", "drive");
//
//        user.createFile("fileName", "skladisteTest", "C:\\Users\\tadic\\Desktop\\novoKreirianFajl.txt", "/txt");
//
//        user.createDir("dir1", "skladisteTest");
//        user.createDir("dir2", "skladisteTest/dir1");
//        user.createDir("dir3", "skladisteTest");
//        user.createDir("dir4", "skladisteTest/dir1/dir2");
//        user.uploadExistingFile("mojNoviFajl","skladisteTest/dir1/dir2/dir4" ,"C:\\Users\\tadic\\Desktop\\aaa.txt","/txt");

    }
}
