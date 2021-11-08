import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import storageSpec.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
        if(!checkName(storageName)){
            System.out.println("File with name: " + storageName +  " already exists. Choose another name.");
            return;
        }
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
    /**
     * Creates directory with given name('String dir') at the specified path(String 'path'); for example createDir("newDir" , "storage/dir1/dir2")
     * When it is created, path of our new directory will be "storage/dir1/dir2/newDir"
     * */
    @Override
    public void createDir(String dir, String path) {
        if(!checkName(dir)){
            System.out.println("File with name: " + dir +  " already exists. Choose another name.");
            return;
        }
        File fileMetadata = new File();
        fileMetadata.setName(dir);
        String folderId = this.findParentDirID(path);
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
     * Uploads existing file with given name, with given type(txt, png...), from pathOnMyComputer(location on local file system) to google drive storage  with given path
     * inside storage->for example: "storage/dir1/dir2"
     * When it is created, path of our file will be "storage/dir1/dir2/myFile"
     */
    @Override
    public void uploadExistingFile(String fileName, String pathWhereToCreateFile,  String pathOnMyComputer, String fileType) {
        String folderId = this.findParentDirID(pathWhereToCreateFile);
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderId));
        java.io.File filePath = new java.io.File(pathOnMyComputer);
        FileContent mediaContent = new FileContent(fileType, filePath);
        File file = null;
        try {
            file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
            file.setName(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File ID: " + file.getId() + "; file name: " + file.getName());
    }
    /**
     * Creates file on specified path on local file system and then uploads it on google drive storage with given path inside storage
     */
    @Override
    public void createFile(String fileName, String pathWhereToUploadFile, String pathOnMyComputer, String fileType) {
        Path filepath = Paths.get(pathOnMyComputer); //creates Path instance
        try
        {
            Path p= Files.createFile(filepath);     //creates empty file at specified location(path) on local file system
            System.out.println("File Created at Path: "+p);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.uploadExistingFile(fileName, pathWhereToUploadFile, pathOnMyComputer, fileType);
    }

    @Override
    public void move(Collection<String> collection, String s) {

    }

    @Override
    public void move(String s, String s1) {

    }

    @Override
    public void delete(String name) {
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if(files == null || files.isEmpty()){
            System.out.println("error in method delete: there is no file with name: " + name );
            return;
        }
        if(files.size() > 1){
            System.out.println("error in method delete: there is more than 1 file with name: " + name);
            return;
        }
        try {
            this.driveService.files().delete(files.get(0).getId()).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void download(String name, String whereToDownload) {
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if(files == null || files.isEmpty()){
            System.out.println("error in method download: there is no file with name: " + name );
            return;
        }
        if(files.size() > 1){
            System.out.println("error in method download: there is more than 1 file with name: " + name);
            return;
        }
        String fileId = files.get(0).getId();
        try {
            OutputStream outputStream = new FileOutputStream(whereToDownload);
            driveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        return null;
    }

    private String findParentDirID(String path){
        String[] array = path.split("/");
        if(array.length == 1){//ako pravimo fajl(folder) direktno u korenksom direktorijumu skladista
            return findStorageID(array[0]);
        }
        String parentName = array[array.length-1];
        String storageName = array[0];
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + parentName + "'")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if(files == null || files.isEmpty()){
            System.out.println("error in method findParentDirID: there is no parent with that name");
            return null;
        }
        if(files.size() > 1){
            System.out.println("error in method findParentDirID: there is more than 1 parent with that name");
            return null;
        }
        return files.get(0).getId();
    }
    private boolean checkName(String name){
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            for (File file : files) {
                if(file.getName().equalsIgnoreCase(name)){
                    return false;
                }
            }
        }
        return true;
    }
}
