import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import storageSpec.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;


public class GoogleDriveUser extends AbstractUser {
    /**
     * Connection to the GoogleDrive
     */
    private final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    private Drive driveService;

    static {
        try {
            UserManager.registerUser(new GoogleDriveUser());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
    public GoogleDriveUser(String userName, String password) throws GeneralSecurityException, IOException {
        super(userName, password);
    }
     */
    public GoogleDriveUser() throws GeneralSecurityException, IOException{
        super.initStoragesAndPrivileges(new HashMap<>());
        driveService = new Drive.Builder(HTTP_TRANSPORT, DriveQuickstart.getJsonFactory(), DriveQuickstart.getCredentials(HTTP_TRANSPORT))
                .setApplicationName(DriveQuickstart.getApplicationName())
                .build();
    }

    /**
     * Creates empty root folder with given storageName at the defined rootLocation on GoogleDrive
     * Created storage is added in map of storages and priveleges.
     */
    @Override
    public void initStorage(String storageName, String rootLocation) {

        File fileMetadata = new File();
        fileMetadata.setName(storageName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = null;
        try {
            file = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            file.setName(storageName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder ID: " + file.getId() + "; " + "folder name: "+ file.getName());

        Storage storage = new Storage(storageName, this, rootLocation, file.getId());
        super.addStorage(storage, Privilege.ADMIN);

    }

    @Override
    public void saveStorageData() {

    }

    @Override
    public void createDir(String dir, String path) {
        File fileMetadata = new File();
        fileMetadata.setName(dir);
        String folderId = this.findStorageID(path);
        fileMetadata.setParents(Collections.singletonList(folderId));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = null;
        try {
            file = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            file.setName(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder ID: " + file.getId() + "; " + "folder name: "+ file.getName());
    }

    @Override
    public void createDir(String s, String s1, String s2, int i) {

    }
    /**
     * Uploads existing file with given name, with given type(txt, png...), from path(location on local file system) to google drive storage with given storageName
     */
    @Override
    public void uploadExistingFile(String fileName, String path, String storageName, String fileType) {
        String folderId = this.findStorageID(storageName);
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderId));
        java.io.File filePath = new java.io.File(path);
        FileContent mediaContent = new FileContent(fileType, filePath);
        File file = null;
        try {
            file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File ID: " + file.getId());
    }
    /**
     * Creates file on specified path on local file system and then uploads it on google drive storage with given storageName
     */
    @Override
    public void createFile(String fileName, String path, String storageName, String fileType) {
        Path filepath = Paths.get(path); //creates Path instance
        try
        {
            Path p= Files.createFile(filepath);     //creates empty file at specified location(path) on local file system
            System.out.println("File Created at Path: "+p);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.uploadExistingFile(fileName, path, storageName, fileType);
    }

    @Override
    public void move(Collection<String> collection, String s) {

    }

    @Override
    public void move(String s, String s1) {

    }

    @Override
    public void delete(String s) {

    }

    @Override
    public void download(String s) {

    }

    @Override
    public Collection<String> searchFilesInDir(String s) {
        return null;
    }

    @Override
    public Collection<String> searchDirsInDir(String s) {
        return null;
    }

    @Override
    public Collection<String> searchByName(String s) {
        return null;
    }

    @Override
    public Collection<String> searchByExtension(String s) {
        return null;
    }

    @Override
    public Collection<String> searchByNameSorted(String s) {
        return null;
    }

    @Override
    public Date getModificationDate(String s) {
        return null;
    }

    @Override
    public Date getCreationDate(String s) {
        return null;
    }

    @Override
    public Collection<String> searchByDateCreationRange(Date date, Date date1) {
        return null;
    }

    @Override
    public Collection<String> searchFilesInDirByDateCreationRange(Date date, Date date1, String s) {
        return null;
    }

    @Override
    public void setStorageSize(int size, Storage storage) {
        if(super.getStoragesAndPrivileges().get(storage) != Privilege.ADMIN){
            System.out.println("Samo admin moze da definise velicinu skladista u bajtovima");
            return;
        }
        storage.setStorageSize(size);
    }

    @Override
    public void setForbiddenExtensions(Collection<String> ext, Storage storage) {
        if(super.getStoragesAndPrivileges().get(storage) != Privilege.ADMIN){
            System.out.println("Samo admin moze da definise koje su zabranjene ekstenzije u skladistu");
            return;
        }
        storage.setForbiddenExtensions(ext);
    }

    @Override
    public void setMaxFileNumberInDir(int i, Storage storage, String dirPath) {

    }

    @Override
    public void addUser(AbstractUser abstractUser, Storage storage, Privilege privilege) {
        if(super.getStoragesAndPrivileges().get(storage) != Privilege.ADMIN){
            System.out.println("Samo admin moze da doda korisnika");
            return;
        }
        storage.addUser(abstractUser);//sluzi da storage zna da ovaj user moze da mu pristupi
        abstractUser.addStorage(storage, privilege);//sluzi da user zna da za ovaj storage ima ovaj nivo privilegije
    }

    @Override
    public void removeUser(AbstractUser abstractUser, Storage storage) {
        if(super.getStoragesAndPrivileges().get(storage) != Privilege.ADMIN){
            System.out.println("Samo admin moze da obrise korisnika");
            return;
        }
        storage.removeUser(abstractUser);
        abstractUser.removeStorage(storage);
    }
    private String findStorageID(String storageName){
        for (Storage storage: super.getStoragesAndPrivileges().keySet()) {
            if(storage.getStorageName().equalsIgnoreCase(storageName)){
                return  storage.getStorageID();
            }
        }
        System.out.println("ERROR: ID for storage with name: " + storageName + " has not been found");
        return "error";
    }
    //here i stopped; i need this method to create file on specified path on google drive
    private String findParentDirID(String storagePath){
        String[] array = storagePath.split("/");

       return null;
    }
}
