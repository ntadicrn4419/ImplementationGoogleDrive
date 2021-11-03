import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import storageSpec.*;

import java.io.IOException;
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

        Storage storage = new Storage(storageName, this, rootLocation);
        super.addStorage(storage, Privilege.ADMIN);

    }

    @Override
    public void saveStorageData() {

    }

    @Override
    public void createDir(String s, String s1) {

    }

    @Override
    public void createDir(String s, String s1, String s2, int i) {

    }

    @Override
    public void createFile(String s, String s1) {

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
    public void setStorageSize(int i, Storage storage) {

    }

    @Override
    public void setForbiddenExtensions(Collection<String> collection, Storage storage) {

    }

    @Override
    public void setMaxFileNumberInDir(int i, Storage storage, Directory directory) {

    }

    @Override
    public void addUser(AbstractUser abstractUser, Storage storage, Privilege privilege) {
        storage.addUser(abstractUser);//sluzi da storage zna da ovaj user moze da mu pristupi
        abstractUser.addStorage(storage, privilege);//sluzi da user zna da za ovaj storage ima ovaj nivo privilegije
    }

    @Override
    public void removeUser(AbstractUser abstractUser, Storage storage) {
        storage.removeUser(abstractUser);
        abstractUser.removeStorage(storage);
    }
}
