	
requirments -> core entoties -> api, -> high level design ->deep dives

requirments 

https://www.hellointerview.com/learn/system-design/problem-breakdowns/dropbox


functional requirments 

upload a file from remote storaeg 
download storage 
sync files across devices
 Users should be able to edit files
Users should be able to view files without downloading them

non functional 
low latency uploadas and downloads
availability (priortize)>> consistency 
support largefiles gbs
resumable uploads
high sync accuracy 

core entities 
files, users

apis
/post/file
{
file,
filemetadata
}
get/files/{fileid}
get/changes/timestamp
-> returnd fileid



1. The client will chunk the file into 5-10Mb pieces and calculate a fingerprint for each chunk. It will also calculate a fingerprint for the entire file, this becomes the fileId.
    
2. The client will send a GET request to fetch the FileMetadata for the file with the given fileId (fingerprint) in order to see if it already exists -- in which case, we can resume the upload.
    
3. If the file does not exist, the client will POST a request to /files/presigned-url to get a presigned URL for the file. The backend will save the file metadata in the FileMetadata table with a status of "uploading" and the chunks array will be a list of the chunk fingerprints with a status of "not-uploaded".
    
4. The client will then upload each chunk to S3 using the presigned URL. After each chunk is uploaded, S3 will send a message to our backend using S3 event notifications. Our backend will then update the chunks field in the FileMetadata table to mark the chunk as "uploaded".
    
5. Once all chunks in our chunks array are marked as "uploaded", the backend will update the FileMetadata table to mark the file as "uploaded".











