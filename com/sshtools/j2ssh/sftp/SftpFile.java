package com.sshtools.j2ssh.sftp;

import java.io.IOException;
/**
 * @author unascribed
 * @version 1.0
 */

public class SftpFile {

  private String filename;
  private byte handle[];
  private FileAttributes attrs;
  private SftpSubsystemClient sftp;
  private String absolutePath;

  protected SftpFile(String absolutePath, FileAttributes attrs) {

    this.absolutePath = absolutePath;
    int i = absolutePath.lastIndexOf("/");

    if(i>-1)
      this.filename = absolutePath.substring(i+1);
    else
      this.filename = absolutePath;

    this.attrs = attrs;
  }

  public void delete() throws IOException {
      if(sftp==null)
        throw new IOException("Instance not connected to SFTP subsystem");

    sftp.removeFile(getAbsolutePath());
  }

    public void rename(String newFilename) throws IOException {
      if(sftp==null)
        throw new IOException("Instance not connected to SFTP subsystem");

     sftp.renameFile(getAbsolutePath() + filename, newFilename);
  }

  public boolean canWrite() {
    return (getAttributes().getPermissions().longValue() & FileAttributes.S_IWUSR) == FileAttributes.S_IWUSR;
  }

  public boolean canRead() {
    return (getAttributes().getPermissions().longValue()  & FileAttributes.S_IRUSR) == FileAttributes.S_IRUSR;
  }

  public boolean isOpen() {
    if(sftp==null)
      return false;

    return sftp.isValidHandle(handle);
  }


  protected void setHandle(byte handle[]) {
    this.handle = handle;
  }

  protected byte[] getHandle() {
    return handle;
  }

  protected void setSFTPSubsystem(SftpSubsystemClient sftp) {
    this.sftp = sftp;
  }

  protected SftpSubsystemClient getSFTPSubsystem() {
    return sftp;
  }

  public String getFilename() {
    return filename;
  }

  private String pad(int num) {
    String str = "";
    if(num>0) {
      for(int i=0;i<num;i++)
        str+=" ";
    }
    return str;
  }

  public String getLongname() {
      StringBuffer str = new StringBuffer();
      str.append(pad(10-getAttributes().getPermissionsString().length()) + getAttributes().getPermissionsString());
      str.append("    1 ");
      str.append(getAttributes().getUID().toString() + pad(8-getAttributes().getUID().toString().length())); //uid
      str.append(" ");
      str.append(getAttributes().getGID().toString() + pad(8-getAttributes().getGID().toString().length())); //gid
      str.append(" ");
      str.append(pad(8-getAttributes().getSize().toString().length()) + getAttributes().getSize().toString());
      str.append(" ");
      str.append(pad(12-getAttributes().getModTimeString().length()) + getAttributes().getModTimeString());
      str.append(" ");
      str.append(filename);
      return str.toString();
  }

  public FileAttributes getAttributes() {
    try {
    if(attrs==null)
      attrs = sftp.getAttributes(this);
    } catch(IOException ioe) {
      attrs = new FileAttributes();
    }
    return attrs;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }

  public void close() throws IOException {
    sftp.closeFile(this);
  }

  public boolean isDirectory() {
    return (getAttributes().getPermissions().intValue()
              & FileAttributes.S_IFDIR)
                == FileAttributes.S_IFDIR;
  }

  public boolean isFile()  {
    return (getAttributes().getPermissions().intValue()
              & FileAttributes.S_IFREG)
                == FileAttributes.S_IFREG;
  }

  public boolean isLink() {
    return (getAttributes().getPermissions().intValue()
              & FileAttributes.S_IFLNK)
                == FileAttributes.S_IFLNK;

  }

  public boolean isFifo()  {
    return (getAttributes().getPermissions().intValue()
              & FileAttributes.S_IFIFO)
                == FileAttributes.S_IFIFO;
  }

  public boolean isBlock()  {
    return (getAttributes().getPermissions().intValue()
              & FileAttributes.S_IFBLK)
                == FileAttributes.S_IFBLK;
  }

  public boolean isCharacter()  {
    return (getAttributes().getPermissions().intValue()
              & FileAttributes.S_IFCHR)
                == FileAttributes.S_IFCHR;
  }

  public boolean isSocket()  {
    return (getAttributes().getPermissions().intValue()
              & FileAttributes.S_IFSOCK)
                == FileAttributes.S_IFSOCK;
  }

}