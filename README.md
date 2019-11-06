# GlacierBackupper

Tool to store backup files, like albums of photos zipped or stuff like that in the cheapest amazon storage option.

Works from command line and keeps a local file to be able to check already files uploaded and to facilitate the download of them, hiding the multi requests that has to be done and hiding big ids behind all glacier operations.

Read first all features (and restrictions) of S3 Glacier: https://aws.amazon.com/glacier/

## Usage

S3 Glacier is not a "live" service. All "operations" must be requested first (jobs), after some hours, the job is completed and result can be read.

*  **Upload a file**

```bash
java -jar GlacierBackupper-1.0.jar -u  -i ~/InventoryGlacierBackup.json -f "/path/to/file/to/backup.zip" -v VaultName -r eu-west-1 -c 32
```
This would upload to the already created vault in your S3 Glacier called "VaultName" on AWS Ireland region the file called backup.zip in chuncks of 32MB

File will be stored with name = file name, in this example example "backup.zip", to override use flag -n.

System keeps an inventory of uploaded files, if you try to upload again same file, systeme won't upload it again and it will inform you.

* **Request a download**

```bash
java -jar GlacierBackupper-1.0.jar -rd -i ~/InventoryGlacierBackup.json -n "backup.zip" 
```
This will request a job in "bulk" tier (about 6-12h to be finished), the cheapest one. For more "urgent" add -u flag (3-5h)

* **List status jobs**

```bash
java -jar GlacierBackupper-1.0.jar -ps -i ~/InventoryGlacierBackup.json
```
To get status of requested jobs. Must not pass more than 24h between a job is completed (so after 6-12h after has been requested) till result is downloaded, other wise job is lost

* **Download**

```bash
java -jar GlacierBackupper-1.0.jar -d -i ~/InventoryGlacierBackup.json -n "backup.zip" -c 32 -t /target/file.zip
```
Downloads backup.zip file to /target/file.zip in chunks of 32MB. 
This file must to be requested first and job been completed before be able to download it.

* **Delete a file**

```bash
java -jar GlacierBackupper-1.0.jar -rm -n "backup.zip" -i ~/InventoryGlacierBackup.json
```
Deletes from Glacier the file "backup.zip"

* **List**

```bash
java -jar GlacierBackupper-1.0.jar -ls -i ~/InventoryGlacierBackup.json
```
List all files uploaded to Glacier accorgind the local inventory. It doesn't request anything to S3 Glacier

## Requirements
- Amazon AWS account
- A vault(s) created
- AWS key and secret (AWS IAm)
- At least, Java 8 installed 

## Authentication
- Passing values by command line with -s and -k to specify awsSecret and awsKey
- Reading credentials from profile (typically located at ~/.aws/credentials)
