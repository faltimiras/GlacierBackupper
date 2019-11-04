# GlacierBackupper

Tool to store backup files, like albums of photos zipped or stuff like that in the cheapest amazon storage option.

Works from command line and keeps a local file to be able to check already files uploaded and to facilitate the download of them, hiding the multi requests that has to be done and hiding big ids behind all glacier operations.

Read first all features (and restrictions) of S3 Glacier: https://aws.amazon.com/glacier/

## Requirements
- Amazon AWS account
- A vault(s) created
- AWS key and secret (AWS IAm)
- At least, Java 8 installed 

## Authentication
- Passing values by command line with -s and -k to specify awsSecret and awsKey
- Reading credentials from profile (typically located at ~/.aws/credentials)

