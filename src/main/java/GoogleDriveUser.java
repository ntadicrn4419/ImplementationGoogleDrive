import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import storageSpec.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    private String defaultLocalFileSystemLocation = "C:\\Users\\tadic\\Desktop";

    static {
        try {
            UserManager.registerUser(new GoogleDriveUser());
            UserManager.registerUserSerializator(new UserSerialization());
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

    @Override
    public boolean storageExists(String storageNameAndPath) {
        return doesStorageExist(storageNameAndPath);
    }

    @Override
    public int logIn(String storageNameAndPath, String username, String password) {
        if(!this.storageHasJsonUsers(storageNameAndPath)){
            return 0;
        }
        if(!this.storageHasJsonMetaDataStorage(storageNameAndPath)){
            return 0;
        }
        ISerialization ser = UserManager.getUserSerializator();
        ((UserSerialization)ser).setDefaultLocalPath(this.defaultLocalFileSystemLocation);
        List<UserData>usersdata = ser.readSavedUsers(storageNameAndPath + "/users.json");

        boolean flag = false;
        for(UserData ud: usersdata){
            if(ud.getUserName().equalsIgnoreCase(username) && ud.getPassword().equals(password)){
                this.setUserName(ud.getUserName());
                this.setPassword(ud.getPassword());
                this.setStoragesAndPrivileges(ud.getStoragesAndPrivileges());
                flag = true;
            }
        }
        if(!flag){
            return 0;
        }
        StorageData storageData = ser.readStorageData(storageNameAndPath + "/storage.json");
        Storage storage = new Storage(storageData.getStorageName(), this, "google drive", storageData.getStorageID());
        storage.setStorageSize(storageData.getStorageSize());
        storage.setForbiddenExtensions(storageData.getForbiddenExtensions());
        this.setCurrentActiveStorage(storage);
        return 1;
    }

    /**
     * Creates empty root folder with given storageName at the defined rootLocation on GoogleDrive
     * Created storage is added in map of storages and priveleges.
     */

    @Override
    public int initStorage(String storageName, String username, String password) {

            this.setUserName(username);
            this.setPassword(password);

            File fileMetadata = new File();
            fileMetadata.setName(storageName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            File file = null;
            try {
                file = driveService.files().create(fileMetadata)
                        .setFields("id, name, mimeType")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Kreiran je storage: " + file.getId() + "; " + "storage name: "+ file.getName());
            Storage storage = new Storage(storageName, this, "google drive", file.getId());
            this.addStorage(file.getId(), Privilege.ADMIN);
            this.setCurrentActiveStorage(storage);

            //kreiramo json file sa userima u ovom storage-u
            this.createUsersJson(this.defaultLocalFileSystemLocation, storageName);
            //kreiramo json file sa meta podacima o storage-u
            this.createStorageJson(this.defaultLocalFileSystemLocation, storageName, storage);

            return 1;
    }
    /**
     * Creates directory with given name('String dir') at the specified path(String 'path'); for example createDir("newDir" , "storage/dir1/dir2")
     * When it is created, path of our new directory will be "storage/dir1/dir2/newDir"
     * */
    @Override
    public int createDir(String dir, String path) {
        if(!checkPrivilege(Privilege.UPLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        if(checkName(dir)){
            System.out.println("File with name: " + dir +  " already exists. Choose another name.");
            return 0;
        }
        String folderId = this.findParentDirID(path);
        if(folderId == null){
            return 0;
        }
        File fileMetadata = new File();
        fileMetadata.setName(dir);
        fileMetadata.setParents(Collections.singletonList(folderId));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        File file = null;
        try {
            file = driveService.files().create(fileMetadata)
                    .setFields("name, id, mimeType, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder ID: " + file.getId() + "; " + "folder name: "+ file.getName());
        return 1;
    }
    /**
    * Creates multiple dirs(numberOfDirs) with given namePrefix
    * */
    @Override
    public int createDir(String dirName, String path, String namePrefix, int numberOfDirs) {

        if(!checkPrivilege(Privilege.UPLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        for(int i = 1; i <= numberOfDirs; i++){
            //createFile(namePrefix + i, path + "/" + dirName, this.defaultLocalFileSystemLocation, "text/plain");
            createDir(namePrefix + i, path + "/" );
        }
        return 1;
    }
    /**
     * Uploads existing file with given name, with given type(txt, png...), from pathOnMyComputer(location on local file system) to google drive storage  with given path
     * inside storage->for example: "storage/dir1/dir2"
     * When it is created, path of our file will be "storage/dir1/dir2/myFile"
     */
    @Override
    public int uploadExistingFile(String fileName, String pathWhereToCreateFile,  String pathOnMyComputer, String fileType) {
        if(!checkPrivilege(Privilege.UPLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
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
            file.setMimeType("application/vnd.google-apps.file");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File ID: " + file.getId() + "; file name: " + file.getName());
        return 1;
    }
    /**
     * Creates file on specified path on local file system and then uploads it on google drive storage with given path inside storage
     */
    @Override
    public int createFile(String fileName, String pathWhereToUploadFile, String pathOnMyComputer, String fileType) {

        if(!checkPrivilege(Privilege.UPLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        if(pathOnMyComputer == null){
           pathOnMyComputer = this.defaultLocalFileSystemLocation;
        }
        Path filepath = Paths.get(pathOnMyComputer + "\\" + fileName); //creates Path instance
        try
        {
            Path p= Files.createFile(filepath);     //creates empty file at specified location(path) on local file system
            System.out.println("File Created at Path: "+p);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.uploadExistingFile(fileName, pathWhereToUploadFile, pathOnMyComputer+ "\\" + fileName, fileType);

        return 1;
    }

    @Override
    public int move(Collection<String> fileCollection, String pathWhereToMoveIt) {
        if(!checkPrivilege(Privilege.DELETE)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        for (String f: fileCollection) {
            move(f, pathWhereToMoveIt);
        }
        return 1;
    }

    @Override
    public int move(String f, String pathWhereToMoveIt) {
        if(!checkPrivilege(Privilege.DELETE)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        //Ukoliko korisnik zada celu putanju, uzima se samo poslednji fajl u putanji, zato sto znamo da su imena jedinstvena na nivou skladista
        String array[] = f.split("/");
        f = array[array.length-1];

        List<File> files = findFileByName(f);
        if(files == null || files.size() == 0 ){
            System.out.println("Error in method move: can not find file with name '" + f + "'");
            return 0;
        }
        if(files.size() > 1){
            System.out.println("Error in method move: more then one file with name '" + f + "'");
            return 0;
        }
        String fileId = files.get(0).getId();

        //Isto kao gore, ako zada celu putanju gde zeli da premesti fajl, radimo isto
        String arr[] = pathWhereToMoveIt.split("/");
        pathWhereToMoveIt = arr[arr.length-1];
        List<File> folders = findFileByName(pathWhereToMoveIt);
        if(folders == null || folders.size() == 0 ){
            System.out.println("Error in method move: can not find folder with name + '" + pathWhereToMoveIt + "'");
            return 0;
        }
        if(folders.size() > 1){
            System.out.println("Error in method move: more then one folder with name + '" + pathWhereToMoveIt + "'");
            return 0;
        }
        String folderId = folders.get(0).getId();

        // Retrieve the existing parents to remove
        File file = null;
        try {
            file = driveService.files().get(fileId)
                    .setFields("parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder previousParents = new StringBuilder();
        for (String parent : file.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
        // Move the file to the new folder
        try {
            file = driveService.files().update(fileId, null)
                    .setAddParents(folderId)
                    .setRemoveParents(previousParents.toString())
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    public int delete(String name) {
        if(!checkPrivilege(Privilege.DELETE)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        FileList result = null;
        String path[] = name.split("/");
        try {
            result = driveService.files().list()
                    .setQ("name = '" + path[path.length-1] + "'")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if(files == null || files.isEmpty()){
            System.out.println("error in method delete: there is no file with name: " + name );
            return 0;
        }
        if(files.size() > 1){
            System.out.println("error in method delete: there is more than 1 file with name: " + name);
            return 0;
        }
        try {
            this.driveService.files().delete(files.get(0).getId()).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }
    //PROBLEM SA PRAZNIM FAjlovima, problem sa slikama(skine ih, al nece da ih otvori), problem sa google docs fajlovima(skine ih, ali random karakteri, necitljivo)
    @Override
    public int download(String name, String whereToDownload) {
        if(!name.contains("storage.json") && !name.contains("users.json")){
            if(!checkPrivilege(Privilege.DOWNLOAD)){
                if(this.getStoragesAndPrivileges().size() != 0){
                    System.out.println("download metoda: Nemate dovoljno visok nivo privilegije za ovu operaciju.");
                    return 0;
                }

            }
        }
        if(whereToDownload == null || whereToDownload == ""){
            whereToDownload = this.defaultLocalFileSystemLocation;
        }
        String array[] = name.split("/");
        name = array[array.length-1];
        List<File> files = findFileByName(name);
        String fileId = null;
        File fl = null;
        for(File f: files){
            //System.out.println("Roditelji: " + f.getParents() + "trazimo: " + array[0]);
            if(f.getParents().contains(this.getFileIdByName(array[0]))){
                fileId = f.getId();
                fl = f;
                break;
            }
        }
        if(fl == null){
            System.out.println("Error in download");
            return 0;
        }
        if(!isFolder(fl)){ // ako nije folder, vec fajl, mozemo odmah da ga download-ujuemo
            try {
                download1(fileId, whereToDownload, name);
            }catch (Exception e1){
                try {
                    download2(fileId, whereToDownload, name);
                }catch (Exception e2){
                    e2.printStackTrace();
                }
            }
            return 0;
        }

        String fileQuery = "'" + fileId + "' in parents and trashed=false";
        FileList childrenList = null;
        try {
            childrenList = driveService.files().list().setFields("nextPageToken, files(id, name, createdTime, mimeType, modifiedTime)").setQ(fileQuery).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
        try {
            Path p = Files.createDirectory(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(File file: childrenList.getFiles()){
            if(isFolder(file)){
                download(file.getName(), whereToDownload + "\\" + name);
                continue;
            }
            try {
                download1(file.getId(), whereToDownload + "\\" + name, file.getName());
            }catch (Exception e1){
                try {
                    download2(file.getId(), whereToDownload + "\\" + name, file.getName());
                }catch (Exception e2){
                    System.out.println("problem sa " + file.getName());
                    e2.printStackTrace();
                }
            }
        }
        return 1;
    }

    //PROBLEM: ne radi kada se prosledi cela putanja, npr: mojeSkladiste/dir1; radi samo sa dir1-->srediti
    //RESENO: ako korisnik unese celu putanju, mi uzimamo samo poslednji naziv zato sto znamo da su imena jedinstvena na nivou skladista
    @Override
    public Collection<String> searchFilesInDir(String dir) {
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }
        String pathToDir[] = dir.split("/");

        FileList result = null;
        String fileId = findFileByName(pathToDir[pathToDir.length-1]).get(0).getId();
        String fileQuery = "'" + fileId + "' in parents and trashed=false and mimeType!='application/vnd.google-apps.folder'";
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name)")
                    .setQ(fileQuery)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        List<String> fileNames = new ArrayList<>();
        for(File f: files){
            fileNames.add(f.getName());
        }
        return fileNames;

    }

    @Override
    public Collection<String> searchDirsInDir(String dir) {
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }
        String pathToDir[] = dir.split("/");

        FileList result = null;
        String fileId = findFileByName(pathToDir[pathToDir.length-1]).get(0).getId();
        String fileQuery = "'" + fileId + "' in parents and trashed=false and mimeType='application/vnd.google-apps.folder'";
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name)")
                    .setQ(fileQuery)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        List<String> fileNames = new ArrayList<>();
        for(File f: files){
            fileNames.add(f.getName());
        }
        return fileNames;
    }

    //OVA METODA NEMA SMISLA...treba da vrati putanju fajla, popraviti!
    @Override
    public Collection<String> searchByName(String name) {//iako vraca kolekciju stringova, vratice uvek kolekciju sa samo jednim clanom, zato sto su imena
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }                                                // jedinstvena na nivou skladista
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name, parents)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    @Override
    public Collection<String> searchByExtension(String extention, String dirPath) {
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }
        String pageToken = null;
        ArrayList fileNames = new ArrayList();
        String fileQuery = "'" + getFileIdByName(dirPath) + "' in parents and mimeType='" + extention + "' and trashed=false";
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setQ(fileQuery)
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : result.getFiles()) {
                fileNames.add(file.getName());
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return fileNames;
    }

    //PROBLEM: Resenja.zip ide pre aaa i pre bbb(folder bla) ; zasto???
    @Override
    public Collection<String> getFilesInDirSortedByName(String dir) {
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }
        String pathToDir[] = dir.split("/");

        FileList result = null;
        String fileId = findFileByName(pathToDir[pathToDir.length-1]).get(0).getId();
        String fileQuery = "'" + fileId + "' in parents and trashed=false";
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name)")
                    .setQ(fileQuery)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        List<String> fileNames = new ArrayList<>();
        for(File f: files){
            fileNames.add(f.getName());
        }
        Collections.sort(fileNames);
        return fileNames;
    }

    @Override
    public String getModificationDate(String path) {
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }
        String array[] = path.split("/");
        FileList result = null;
        String s = array[array.length-1];
        try {
            result = driveService.files().list()
                    .setQ("name='" + s + "'")
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType, modifiedTime)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        if (files.size() > 1) {
            System.out.println("Error: more than one file with that name.");
            return null;
        }
        return files.get(0).getModifiedTime().toString();
    }

    @Override
    public String getCreationDate(String path) {
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }
        String array[] = path.split("/");
        FileList result = null;
        String s = array[array.length-1];
        try {
            result = driveService.files().list()
                    .setQ("name='" + s + "'")
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType, modifiedTime)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        if (files.size() > 1) {
            System.out.println("Error: more than one file with that name.");
            return null;
        }
        System.out.println(files.get(0).getCreatedTime());
        return files.get(0).getCreatedTime().toString();
    }

    //odnosi se na celo skladiste
    @Override
    public Collection<String> searchByDateCreationRange(Date date, Date date1) {

        return null;
    }
    //PROBLEM: Ne radi kako treba, izmeniti skroz!!!
    @Override
    public Collection<String> searchFilesInDirByDateCreationRange(Date date1, Date date2, String dir) {
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }
        List<File> files = getFileObjectsInDir(dir);
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return null;
        }
        List<String> result = new ArrayList<>();
        for(File f: files){
            DateTime dt = f.getCreatedTime();
            if(dt.getValue() > date1.getTime() && dt.getValue() < date2.getTime()){
                result.add(f.getName());
            }
        }
        return result;
    }

    @Override
    public int setStorageSize(int size) {

        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        this.getCurrentActiveStorage().setStorageSize(size);


        return 1;
    }

    @Override
    public int setForbiddenExtensions(Collection<String> ext) {
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        this.getCurrentActiveStorage().setForbiddenExtensions(ext);
        return 1;
    }

    @Override
    public int setMaxFileNumberInDir(int i, String dirPath) {
        //naci id foldera by name, this.currentACtiveStorage.getMap.add(id, i)
        return 0;
    }
    @Override
    public int addUser(String userName, String password, Privilege privilege) {
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        List<UserData> usersData = UserManager.getUserSerializator().readSavedUsers(this.getCurrentActiveStorage().getStorageName() + "/users.json");

        Map<String, Privilege> map = new HashMap<>();
        map.put(this.getCurrentActiveStorage().getStorageID(), privilege);

        Path filepath = Paths.get(this.defaultLocalFileSystemLocation + "\\" + "users.json");
        try
        {
            Path p= Files.createFile(filepath);
            ISerialization ser = UserManager.getUserSerializator();
            for(UserData userData: usersData){ // prvo sacuvamo ono sto je bilo pre
                ser.saveUserData(String.valueOf(filepath),userData.getUserName(), userData.getPassword(), userData.getStoragesAndPrivileges(), true);
            }
            ser.saveUserData(String.valueOf(filepath),userName, password, map, true);//dodamo ovo novo
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //this.delete("users.json");-->brisemo sa drajva onaj stari file sa userima->za to moramo prvo da popravimo metodu delete
        this.uploadExistingFile("users.json", this.getCurrentActiveStorage().getStorageName(),this.defaultLocalFileSystemLocation + "\\" + "users.json", "application/json");
        try {
            Files.delete(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }
    @Override
    public int removeUser(String userName) {
        //readUsers, izbrisati onog sa ovim userName, tu listu ponovo savovati u petlji, prva iteracija
        // appent false, sve ostalo append true
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return 0;
        }
        List<UserData> usersData = UserManager.getUserSerializator().readSavedUsers(this.getCurrentActiveStorage().getStorageName() + "/users.json");
        for (UserData ud: usersData){
            if(ud.getUserName().equalsIgnoreCase(userName)){
                usersData.remove(ud);
                break;
            }
        }
        Path filepath = Paths.get(this.defaultLocalFileSystemLocation + "\\" + "users.json");
        try
        {
            Path p= Files.createFile(filepath);
            ISerialization ser = UserManager.getUserSerializator();
            int cnt = 0;
            for(UserData userData: usersData){
                ser.saveUserData(String.valueOf(filepath),userData.getUserName(), userData.getPassword(), userData.getStoragesAndPrivileges(), true);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //this.delete("users.json");-->brisemo sa drajva onaj stari file sa userima->za to moramo prvo da popravimo metodu delete
        this.uploadExistingFile("users.json", this.getCurrentActiveStorage().getStorageName(),this.defaultLocalFileSystemLocation + "\\" + "users.json", "application/json");
        try {
            Files.delete(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }
    private List<File> getFileObjectsInDir(String dir){
        String pathToDir[] = dir.split("/");
        FileList result = null;
        String fileId = findFileByName(pathToDir[pathToDir.length-1]).get(0).getId();
        String fileQuery = "'" + fileId + "' in parents and trashed=false";
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType, modifiedTime)")
                    .setQ(fileQuery)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(result == null){
            return null;
        }
        List<File> files = result.getFiles();
        return files;

    }
    private boolean isFolder(File file){

        if(file.getMimeType() != null && file.getMimeType().equalsIgnoreCase("application/vnd.google-apps.folder")){
            return true;
        }
        return false;
    }
    private void download1(String fileId, String whereToDownload, String name) throws IOException {//za obicne(npr .txt )fajlove
        OutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream);
        Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
        Path p = Files.createFile(filepath);
        List<String> lines = Arrays.asList(outputStream.toString());
        Files.write(Paths.get(whereToDownload + "\\" + name), lines, StandardCharsets.UTF_8);
    }
    private void download2(String fileId, String whereToDownload, String name) throws IOException {//za google docs fajlove
        OutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().export(fileId, "application/zip").executeMediaAndDownloadTo(outputStream);
        Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
        Path p= Files.createFile(filepath);
        List<String> lines = Arrays.asList(outputStream.toString());
        Files.write(Paths.get(whereToDownload + "\\" + name), lines);
        outputStream.close();
    }

    private String findParentDirID(String path){
        String[] array = path.split("/");
        String parentName = array[array.length-1];
        //String storageName = array[0];
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


    private boolean doesStorageExist(String name){
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name, parents)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return false;
        }
        for (File f: files){
            if(f.getName().equalsIgnoreCase(name)){// && f.getParents.contatins("rootStorage")
                return true;
            }
        }
        return false;
    }
    private boolean checkName(String name){
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setFields("nextPageToken, files(id, name, parents)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return false;
        }
        for (File f: files){
            if(f.getName().equalsIgnoreCase(name)){
                return true;
            }
        }
        return false;
    }
    private String getFileIdByName(String storageName){
        FileList result = null;
        List<File> files = findFileByName(storageName);
        if(files.size() > 1 || files.size() == 0){
            System.out.println("error in getFileIdByName");
            return null;
        }
        String storageId = files.get(0).getId();
        return storageId;
    }
    //PROBLEM: sta ako se prosledi cela putanja, a ne samo ime fajla: npr: myStorage/dir1/file1
    //RESENJE: pre poziva ove metode uvek se parsira ulazni string tako da se ovoj metodi uvek prosledi samo ime fajla, a ne cela putanja
    public List<File> findFileByName(String name){
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType, modifiedTime, size, parents, contentHints)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        return files;
    }
    /**
     * Checks if this user has enough level of privilege(for currentActiveStorage) for operation that needs given minimumPrivilegeLevel
     * */
    private boolean checkPrivilege(Privilege minimumPrivilegeLevel){
        Privilege privilege = null;
        for (String stID: super.getStoragesAndPrivileges().keySet()) {
            if(stID.equalsIgnoreCase(super.getCurrentActiveStorage().getStorageID())){
               privilege = super.getStoragesAndPrivileges().get(stID);
            }
        }
        if(privilege != null && privilege.ordinal() >= minimumPrivilegeLevel.ordinal()){
            return true;
        }
        return false;
    }
    private boolean storageHasJsonUsers(String storageName){
        FileList result = null;
        List<File> files = findFileByName(storageName);
        if(files.size() > 1 || files.size() == 0){
            System.out.println("Error in storageHasJsonUsers");
            return false;
        }
        String storageId = files.get(0).getId();
        String fileQuery = "'" + storageId + "' in parents and trashed=false";
        try {
            result = driveService.files().list()
                    .setQ(fileQuery)
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType, modifiedTime, size)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(File file: result.getFiles()){
            if(file.getName().equalsIgnoreCase("users.json")){
                return true;
            }
        }
        return false;
    }
    private boolean storageHasJsonMetaDataStorage(String storageName){
        FileList result = null;
        List<File> files = findFileByName(storageName);
        if(files.size() > 1 || files.size() == 0){
            System.out.println("Error in storageHasJsonMetaDataStorage");
            return false;
        }
        String storageId = files.get(0).getId();
        String fileQuery = "'" + storageId + "' in parents and trashed=false";
        try {
            result = driveService.files().list()
                    .setQ(fileQuery)
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType, modifiedTime, size)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(File file: result.getFiles()){
            if(file.getName().equalsIgnoreCase("storage.json")){
                return true;
            }
        }
        return false;
    }
    private void createUsersJson(String pathOnMyComputer, String pathWhereToUploadFile){
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        if(pathOnMyComputer == null || pathOnMyComputer == ""){
            pathOnMyComputer = this.defaultLocalFileSystemLocation;
        }
        Path filepath = Paths.get(pathOnMyComputer + "\\" + "users.json"); //creates Path instance
        try
        {
            Path p= Files.createFile(filepath);
            ISerialization ser = UserManager.getUserSerializator();
            ser.saveUserData(String.valueOf(filepath),this.getUserName(), this.getPassword(), this.getStoragesAndPrivileges(), false);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.uploadExistingFile("users.json", pathWhereToUploadFile, pathOnMyComputer+ "\\" + "users.json", "application/json");
        try {
            Files.delete(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void createStorageJson(String pathOnMyComputer, String pathWhereToUploadFile, Storage storage){
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        if(pathOnMyComputer == null || pathOnMyComputer == ""){
            pathOnMyComputer = this.defaultLocalFileSystemLocation;
        }
        Path filepath = Paths.get(pathOnMyComputer + "\\" + "storage.json"); //creates Path instance
        try
        {
            Path p= Files.createFile(filepath);
            ISerialization ser = UserManager.getUserSerializator();
            ser.saveStorageData(String.valueOf(filepath), storage);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.uploadExistingFile("storage.json", pathWhereToUploadFile, pathOnMyComputer+ "\\" + "storage.json", "application/json");
        try {
            Files.delete(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
