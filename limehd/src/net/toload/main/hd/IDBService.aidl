package net.toload.main.hd;

interface IDBService
{
	void loadMapping(String filename, String tablename);
	void resetMapping(String tablename);
	void resetDownloadDatabase();
	void downloadDayi();
	void downloadPhonetic();
	void downloadPhoneticCns();
	void downloadPhoneticAdv();
	void downloadCj();
	void downloadCjCns();
	void downloadScj();
	void downloadCj5();
	void downloadEcj();
	void downloadArray();
	void downloadArray10();
	void downloadWb();
	void downloadHs();
	void downloadEz();
	void downloadPreloadedDatabase();
	void downloadPhoneticOnlyDatabase();
	void downloadPhoneticHsOnlyDatabase();
	void downloadEmptyDatabase();
	void backupDatabase();
	void restoreDatabase();
	void forceUpgrad();
	void resetImInfo(String im);
	void removeImInfo(String im, String field);
	void setImInfo(String im, String field, String value);
	void setIMKeyboard(String im, String value,String keyboard);
	void closeDatabse();
	String getImInfo(String im, String field);
	String getKeyboardCode(String im);
	List getKeyboardList();
	String getKeyboardInfo(String keyboardCode, String field);
	int	getLoadingMappingCount();
	int getLoadingMappingPercentageDone();
	boolean isLoadingMappingFinished();
	boolean isLoadingMappingThreadAborted();
	boolean isLoadingMappingThreadAlive();
	boolean isRemoteFileDownloading();
	void abortLoadMapping();
	void abortRemoteFileDownload();
	
}