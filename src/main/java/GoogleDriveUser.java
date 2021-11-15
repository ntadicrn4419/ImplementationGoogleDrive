import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import storageSpec.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.*;

//Proveravamo privilegiju na pocetku svake operacije. Da bi to sad radilo moramo da
// 1)imamo setovano polje currentActiveStorage
// 2)ulogujemo se kao user-->KAKO TO??!!!!
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
            file.setMimeType("application/vnd.google-apps.folder");//dodato
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
        if(!checkPrivilege(Privilege.UPLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
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
            file.setMimeType("application/vnd.google-apps.folder");//dodato
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder ID: " + file.getId() + "; " + "folder name: "+ file.getName());
    }
    /**
    * Creates dir. In dir creates numberOfFiles files. Example: if namePrefix is myFile and numberOfFiles is 3. In dir will be created 3 empty files:
     * myFile1 , myFile2, myFile3
    * */
    @Override
    public void createDir(String dirName, String path, String namePrefix, int numberOfFiles) {
        if(!checkPrivilege(Privilege.UPLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        this.createDir(dirName, path);
        List<File> files = this.findFileByName(dirName);

        File myNewDir = files.get(0);
        for(int i = 1; i <= numberOfFiles; i++){
            createFile(namePrefix + i, path + "/myNewDir", this.defaultLocalFileSystemLocation, "text/plain");
        }

    }
    /**
     * Uploads existing file with given name, with given type(txt, png...), from pathOnMyComputer(location on local file system) to google drive storage  with given path
     * inside storage->for example: "storage/dir1/dir2"
     * When it is created, path of our file will be "storage/dir1/dir2/myFile"
     */
    @Override
    public void uploadExistingFile(String fileName, String pathWhereToCreateFile,  String pathOnMyComputer, String fileType) {
        if(!checkPrivilege(Privilege.UPLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
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
    }
    /**
     * Creates file on specified path on local file system and then uploads it on google drive storage with given path inside storage
     */
    @Override
    public void createFile(String fileName, String pathWhereToUploadFile, String pathOnMyComputer, String fileType) {
        if(!checkPrivilege(Privilege.UPLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
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
    }

    @Override
    public void move(Collection<String> fileCollection, String pathWhereToMoveIt) {
        if(!checkPrivilege(Privilege.DELETE)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        for (String f: fileCollection) {
            move(f, pathWhereToMoveIt);
        }
    }

    @Override
    public void move(String f, String pathWhereToMoveIt) {
        if(!checkPrivilege(Privilege.DELETE)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        //Ukoliko korisnik zada celu putanju, uzima se samo poslednji fajl u putanji, zato sto znamo da su imena jedinstvena na nivou skladista
        String array[] = f.split("/");
        f = array[array.length-1];

        List<File> files = findFileByName(f);
        if(files == null || files.size() == 0 ){
            System.out.println("Error in method move: can not find file with name '" + f + "'");
            return;
        }
        if(files.size() > 1){
            System.out.println("Error in method move: more then one file with name '" + f + "'");
            return;
        }
        String fileId = files.get(0).getId();

        //Isto kao gore, ako zada celu putanju gde zeli da premesti fajl, radimo isto
        String arr[] = pathWhereToMoveIt.split("/");
        pathWhereToMoveIt = arr[arr.length-1];
        List<File> folders = findFileByName(pathWhereToMoveIt);
        if(folders == null || folders.size() == 0 ){
            System.out.println("Error in method move: can not find folder with name + '" + pathWhereToMoveIt + "'");
            return;
        }
        if(folders.size() > 1){
            System.out.println("Error in method move: more then one folder with name + '" + pathWhereToMoveIt + "'");
            return;
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
    }

    @Override
    public void delete(String name) {
        if(!checkPrivilege(Privilege.DELETE)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name, size, mimeType, creationDate, modifiedDate)")
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
    //PROBLEM SA PRAZNIM FAjlovima, problem sa slikama(skine ih, al nece da ih otvori), problem sa google docs fajlovima(skine ih, ali random karakteri, necitljivo)
    @Override
    public void download(String name, String whereToDownload) {
        if(!checkPrivilege(Privilege.DOWNLOAD)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        if(whereToDownload == null || whereToDownload == ""){
            whereToDownload = this.defaultLocalFileSystemLocation;
        }
        String array[] = name.split("/");
        name = array[array.length-1];
        List<File> files = findFileByName(name);
        if(files == null || files.isEmpty()){
            System.out.println("error in method download: there is no file with name: " + name );
            return;
        }
        if(files.size() > 1){
            System.out.println("error in method download: there is more than 1 file with name: " + name);
            return;
        }
        File f  = files.get(0);
        String fileId = f.getId();

        if(!isFolder(f)){ // ako nije folder, vec fajl, mozemo odmah da ga download-ujuemo
            try {
                download1(fileId, whereToDownload, name);
            }catch (Exception e1){
                try {
                    download2(fileId, whereToDownload, name);
                }catch (Exception e2){
                    e2.printStackTrace();
                }
            }
            return;
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
    public Collection<String> searchByExtension(String extention) {
        if(!checkPrivilege(Privilege.READ)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return null;
        }
        String pageToken = null;
        ArrayList fileNames = new ArrayList();
        do {
            FileList result = null;
            try {
                result = driveService.files().list()
                        .setQ("mimeType='" + extention + "'")
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
    public Object getModificationDate(String path) {
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
        return files.get(0).getModifiedTime();
    }

    @Override
    public Object getCreationDate(String path) {
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
        return files.get(0).getCreatedTime();
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
    public void setStorageSize(int size, Storage storage) {
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        storage.setStorageSize(size);
    }

    @Override
    public void setForbiddenExtensions(Collection<String> ext, Storage storage) {
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        storage.setForbiddenExtensions(ext);
    }

    @Override
    public void setMaxFileNumberInDir(int i, Storage storage, String dirPath) {

    }

    @Override
    public void addUser(AbstractUser abstractUser, Storage storage, Privilege privilege) {
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        storage.addUser(abstractUser);//sluzi da storage zna da ovaj user moze da mu pristupi
        abstractUser.addStorage(storage, privilege);//sluzi da user zna da za ovaj storage ima ovaj nivo privilegije
    }

    @Override
    public void removeUser(AbstractUser abstractUser, Storage storage) {
        if(!checkPrivilege(Privilege.ADMIN)){
            System.out.println("Nemate dovoljno visok nivo privilegije za ovu operaciju.");
            return;
        }
        storage.removeUser(abstractUser);
        abstractUser.removeStorage(storage);
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
        outputStream.flush();
        Path filepath = Paths.get(whereToDownload + "\\" + name); //creates Path instance
        Path p= Files.createFile(filepath);
        List<String> lines = Arrays.asList(outputStream.toString());
        Files.write(Paths.get(whereToDownload + "\\" + name), lines);
        outputStream.close();
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
    //PROBLEM: sta ako se prosledi cela putanja, a ne samo ime fajla: npr: myStorage/dir1/file1
    //RESENJE: pre poziva ove metode uvek se parsira ulazni string tako da se ovoj metodi uvek prosledi samo ime fajla, a ne cela putanja
    private List<File> findFileByName(String name){
        FileList result = null;
        try {
            result = driveService.files().list()
                    .setQ("name = '" + name + "'")
                    .setFields("nextPageToken, files(id, name, createdTime, mimeType, modifiedTime, size)")
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
        for (Storage st: super.getStoragesAndPrivileges().keySet()) {
            if(st.getStorageID().equalsIgnoreCase(super.getCurrentActiveStorage().getStorageID())){
               privilege = super.getStoragesAndPrivileges().get(st);
            }
        }
        if(privilege != null && privilege.ordinal() >= minimumPrivilegeLevel.ordinal()){
            return true;
        }
        return false;
    }
}
