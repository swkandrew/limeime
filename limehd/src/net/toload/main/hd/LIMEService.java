/*    
 **    Copyright 2010, The LimeIME Open Source Project
 ** 
 **    Project Url: http://code.google.com/p/limeime/
 **                 http://android.toload.net/
 **
 **    This program is free software: you can redistribute it and/or modifyf
 **    it under the terms of the GNU General Public License as published by
 **    the Free Software Foundation, either version 3 of the License, or
 **    (at your option) any later version.

 **    This program is distributed in the hope that it will be useful,
 **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **    GNU General Public License for more details.

 **    You should have received a copy of the GNU General Public License
 **    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.toload.main.hd;

import android.graphics.Rect;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.toload.main.hd.keyboard.LIMEBaseKeyboard;
import net.toload.main.hd.keyboard.LIMEKeyboard;
import net.toload.main.hd.keyboard.LIMEKeyboardBaseView;
import net.toload.main.hd.keyboard.LIMEKeyboardView;
import net.toload.main.hd.keyboard.LIMEMetaKeyKeyListener;
import net.toload.main.hd.R;
import net.toload.main.hd.candidate.CandidateView;
import net.toload.main.hd.candidate.CandidateViewContainer;
import net.toload.main.hd.global.ChineseSymbol;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.Mapping;
import net.toload.main.hd.limedb.ExpandableDictionary;
import net.toload.main.hd.limedb.UserDictionary;
import net.toload.main.hd.limesettings.LIMEPreference;
import net.toload.main.hd.limesettings.LIMEPreferenceHC;
import android.annotation.TargetApi;
import android.app.AlertDialog;
 
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;

/**
 * @author Art Hung
 */
public class LIMEService extends InputMethodService implements
					LIMEKeyboardBaseView.OnKeyboardActionListener {

	static final boolean DEBUG = false;
	static final String TAG = "LIMEService";
	static final String PREF = "LIMEXY";

	static final int KEYBOARD_SWITCH_CODE = -9;
	static final int KEYBOARD_SWITCH_IM_CODE = -10;

	private LIMEKeyboardView mInputView = null;
	private CandidateViewContainer mCandidateViewContainer;
	private CandidateView mCandidateView = null;
	private CompletionInfo[] mCompletions;

	private StringBuilder mComposing = new StringBuilder();

	//private boolean isModeURL = false;
	//private boolean isModePassword = false;
	private boolean mPredictionOn;
	private boolean mCompletionOn;
	private boolean mCapsLock;
	private boolean mAutoCap;
	//private boolean mQuickFixes;
	private boolean mHasShift;

	private boolean mEnglishOnly;
	private boolean mEnglishFlagShift;
	private boolean mPersistentLanguageMode;
	
	//private boolean mPredictionOnPhysicalKeyboard = false;

	//private boolean onIM = true;  Jeremy '12,4,29 use mEnglishOnly instead
	//private boolean hasFirstMatched = false;

	// if getMapping result has record then set to 'true'
	public boolean hasMappingList = false;

	//private boolean keydown = false;

	private long mMetaState;
	//private boolean mJustAccepted;
	//private CharSequence mJustRevertedSeparator;
	private int mImeOptions;

	LIMEKeyboardSwitcher mKeyboardSwitcher;


	private UserDictionary mUserDictionary;
	//private ContactsDictionary mContactsDictionary;
	//private ExpandableDictionary mAutoDictionary;
	

	//private boolean mAutoSpace;
	//private boolean mAutoCorrectOn;
	//private boolean mShowSuggestions;
	//private int mCorrectionMode;
	private int mOrientation;
	private int mHardkeyboardHidden;
	private boolean mPredicting;
	//private String mLocale;
	//private int mDeleteCount;

	//private Suggest mSuggest;

	//private String mSentenceSeparators;

	private Mapping firstMatched;
	private Mapping tempMatched;

	private StringBuffer tempEnglishWord;
	private List<Mapping> tempEnglishList;

	private boolean isPhysicalKeyPressed;

	//private String mWordSeparators;
	private String misMatched;

	private LinkedList<Mapping> templist;
	//private LinkedList<Mapping> userdiclist;

	private Vibrator mVibrator;
	private AudioManager mAudioManager;

	private final float FX_VOLUME = 1.0f;
	

	private boolean hasVibration = false;
	private boolean hasSound = false;
	// private boolean hasNumberKeypads = false;
	private boolean hasNumberMapping = false;
	private boolean hasSymbolMapping = false;
	//private boolean hasKeyPress = false;
	private boolean hasQuickSwitch = false;

	// Hard Keyboad Shift + Space Status
	private boolean hasShiftPress = false;
	//private boolean hasShiftProcessed = false; // Jeremy '11,6.18
	private boolean hasCtrlPress = false; // Jeremy '11,5,13
	private boolean hasWinPress = false; // Jeremy '12,4,29 windows start key on stadard windows keyboard
	//private boolean hasCtrlProcessed = false; // Jeremy '11,6.18
	private boolean hasDistinctMultitouch;// Jeremy '11,8,3 
	private boolean hasShiftCombineKeyPressed = false; //Jeremy ,11,8, 3
	private boolean hasMenuPress = false; // Jeremy '11,5,29
	private boolean hasMenuProcessed = false; // Jeremy '11,5,29
	//private boolean hasSearchPress = false; // Jeremy '11,5,29
	//private boolean hasSearchProcessed = false; // Jeremy '11,5,29
	
	private boolean hasEnterProcessed = false; // Jeremy '11,6.18
	private boolean hasSpaceProcessed = false;
	private boolean hasKeyProcessed = false; // Jeremy '11,8,15 for long pressed key
	private int mLongPressKeyTimeout; //Jeremy '11,8, 15 read long press timeout from config
	
	private boolean hasSymbolEntered = false; //Jeremy '11,5,24 

	// private boolean hasSpacePress = false;

	// Hard Keyboad Shift + Space Status
	//private boolean hasAltPress = false;
	
	private String mActiveKeyboardState=""; // Jeremy '11,8,5
	public String activeIMCode;  //Jeremy '12,4,30 renamed from keyboardSelection
	private List<String> activeIMNameList; //Jeremy '12,4,30 renamed from keyboardList
	private List<String> activeIMShortNameList; //Jeremy '12,4,30 renamed from keyboardShortname
	private List<String> activeIMCodeList; //jerem '12,4,30 reanmed from keybaordCodeList
	private String activeSoftKeyboard = "";  //Jeremy '12,4,30 reanmed from keybaord_xml;

	

	// To keep key press time
	private long keyPressTime = 0;

	// Keep keydown event
	KeyEvent mKeydownEvent = null;

	//private int previousKeyCode = 0;
	//private final float moveLength = 15;
	//private ISearchService SearchSrv = null;
	private SearchServer SearchSrv = null;

	static final int FREQUENCY_FOR_AUTO_ADD = 250;

	// Weight added to a user picking a new word from the suggestion strip
	static final int FREQUENCY_FOR_PICKED = 3;
	
	// Auto Commmit Value
	private int auto_commit = 0;
	
	
	// Disable physical keyboard candidate words selection
	private boolean disable_physical_selection = false;
	
	// Replace Keycode.KEYCODE_CTRL_LEFT/RIGHT, ESC on android 3.x
	// for backward compatibility of 2.x
	static final int MY_KEYCODE_ESC = 111;
	static final int MY_KEYCODE_CTRL_LEFT = 113;
	static final int MY_KEYCODE_CTRL_RIGHT = 114;
	static final int MY_KEYCODE_ENTER = 10;
	static final int MY_KEYCODE_SPACE = 32;
	static final int MY_KEYCODE_SWITCH_CHARSET = 95;
	static final int MY_KEYCODE_WINDOWS_START = 117; //Jeremy '12,4,29 windows start key
	
	private final String relatedSelkey = "!@#$%^&*()";
	
	private String LDComposingBuffer=""; //Jeremy '11,7,30 for learning continuous typing phrases 

	private LIMEPreferenceManager mLIMEPref;
	
	private boolean isChineseSymbolSuggestionsShowing= false;

	
	
	/*
	 * Construct SerConn
	 *
	private ServiceConnection serConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			SearchSrv = ISearchService.Stub.asInterface(service);
			try {
				SearchSrv.initial();
				initialViewAndSwitcher();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName name) {
		}
	};
	*/
	/**
	 * Main initialization of the input method component. Be sure to call to
	 * super class.
	 */
	@Override
	public void onCreate() {

		if(DEBUG) Log.i(TAG, "OnCreate()");
		
		super.onCreate();
		
		SearchSrv = new SearchServer(this);
		//SearchSrv.initial();

		initialViewAndSwitcher();
        //mKeyboardSwitcher = new LIMEKeyboardSwitcher(this); 
		mEnglishOnly = false;
		mEnglishFlagShift = false;

		// Startup Service
		/*
		if (SearchSrv == null) {
			try {
				this.bindService(new Intent(ISearchService.class.getName()),
						serConn, Context.BIND_AUTO_CREATE);
			} catch (Exception e) {
				Log.i(TAG, "OnCreate(): Failed to connect Search Service");
			}
		}
		*/
		// Construct Preference Access Tool
		mLIMEPref = new LIMEPreferenceManager(this);

		mVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
		//getApplication().getSystemService(Service.VIBRATOR_SERVICE);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		
		mLongPressKeyTimeout = getResources().getInteger(R.integer.config_long_press_key_timeout); // Jeremy '11,8,15 read longpress timeout from config resources.
		if(DEBUG) Log.i(TAG,"OnCreate(): mLongPressKeyTimeout=" + mLongPressKeyTimeout);
		//SharedPreferences sp = PreferenceManager
		//		.getDefaultSharedPreferences(this);
		hasVibration = mLIMEPref.getVibrateOnKeyPressed();//sp.getBoolean("vibrate_on_keypress", false);
		hasSound = mLIMEPref.getSoundOnKeyPressed();//sp.getBoolean("sound_on_keypress", false);
		mPersistentLanguageMode = mLIMEPref.getPersistentLanguageMode(); //sp.getBoolean("default_in_english", false);
		// hasNumberKeypads = sp.getBoolean("display_number_keypads", false);
		activeIMCode = mLIMEPref.getActiveIM();// sp.getString("keyboard_list", "custom");

		// initial Input List
		//userdiclist = new LinkedList<Mapping>();

		// initial keyboard list
		activeIMNameList = new ArrayList<String>();
		activeIMCodeList = new ArrayList<String>();
		activeIMShortNameList = new ArrayList<String>();
		buildActiveIMList();

		

	}

	/*
	private void initSuggest(String locale) {
		mLocale = locale;
		// mSuggest = new Suggest(this, R.raw.main);
		mSuggest.setCorrectionMode(mCorrectionMode);
		mUserDictionary = new UserDictionary(this);
		mContactsDictionary = new ContactsDictionary(this);
		mAutoDictionary = new AutoDictionary(this);
		mSuggest.setUserDictionary(mUserDictionary);
		mSuggest.setContactsDictionary(mContactsDictionary);
		mSuggest.setAutoDictionary(mAutoDictionary);
		mWordSeparators = getResources().getString(R.string.word_separators);
		mSentenceSeparators = getResources().getString(
				R.string.sentence_separators);
	}
	*/
	/**
	 * This is the point where you can do all of your UI initialization. It is
	 * called after creation and any configuration change.
	 */
	@Override
	public void onInitializeInterface() {

		if(DEBUG)
			Log.i(TAG, "onInitializeInterface()");
		//mEnglishOnly = false;
		mEnglishFlagShift = false;

		initialViewAndSwitcher();

		mKeyboardSwitcher.makeKeyboards(true);
		super.onInitializeInterface();

	}
	/**
	 * Called by the system when the device configuration changes while your activity is running.
	 * 
	 */
	@Override
	public void onConfigurationChanged(Configuration conf) {

		if (DEBUG)
			Log.i(TAG, "LIMEService:OnConfigurationChanged()");

		//if (!TextUtils.equals(conf.locale.toString(), mLocale)) {
			// initSuggest(conf.locale.toString());
		//}
		
		//Jeremy '12,4,7 add hardkeyboard hidden configuration changed event and clear composing to avoid fc.
		if (conf.orientation != mOrientation || conf.hardKeyboardHidden != mHardkeyboardHidden) {
			//Jeremy '12,4,21 foce clear the composing buffer
			clearComposing(true);
			
			
			mOrientation = conf.orientation;
			mHardkeyboardHidden = conf.hardKeyboardHidden;
		}
		initialViewAndSwitcher();
		mKeyboardSwitcher.makeKeyboards(true);
		super.onConfigurationChanged(conf);

	}

	/**
	 * Called by the framework when your view for creating input needs to be
	 * generated. This will be called the first time your input method is
	 * displayed, and every time it needs to be re-created such as due to a
	 * configuration change.
	 */
	@Override
	public View onCreateInputView() {
		
		
		mInputView = (LIMEKeyboardView) getLayoutInflater().inflate(
				R.layout.input, null);
		mKeyboardSwitcher.setInputView(mInputView);
		mKeyboardSwitcher.makeKeyboards(true);
		// build activekeyboard list and store in keybaord switcher.
		buildActiveIMList();
		mKeyboardSwitcher.setActiveKeyboardList(activeIMCodeList, activeIMNameList, activeIMShortNameList);
		mInputView.setOnKeyboardActionListener(this);
		hasDistinctMultitouch = mInputView.hasDistinctMultitouch();
		
		if (DEBUG)
			Log.i(TAG, "onCreateInputView(), hasDistinctMultitouch= " + hasDistinctMultitouch);

		//mKeyboardSwitcher.setKeyboardMode(keyboardSelection, LIMEKeyboardSwitcher.MODE_TEXT,
		//		EditorInfo.IME_ACTION_NEXT, true, false, false);

		// mKeyboardSwitcher.setKeyboardMode(
		// LIMEKeyboardSwitcher.MODE_TEXT_DEFAULT, 0);

		//initialKeyboard();
		initialViewAndSwitcher();  //Jeremy '12,4,29.  will do buildactivekeyboardlist in init startInput
		
		return mInputView;

	}
	/**
	 * Create and return the view hierarchy used to show candidates. 
	 * This will be called once, when the candidates are first displayed. 
	 * You can return null to have no candidates view; the default implementation returns null.
	 * 
	 */
	@Override
	public View onCreateCandidatesView() {
		
		if (DEBUG)
			Log.i(TAG,"onCreateCandidatesView()");
		mKeyboardSwitcher.makeKeyboards(true);
		mCandidateViewContainer = (CandidateViewContainer) getLayoutInflater().inflate(
				R.layout.candidates, null);
		mCandidateViewContainer.initViews();
		mCandidateView = (CandidateView) mCandidateViewContainer.findViewById(R.id.candidates);
		//mCandidateView = new CandidateView(this);
		mCandidateView.setService(this);
		//clearComposing();
		return mCandidateViewContainer;
		
		
	}

	/** 
	 * Override this to control when the input method should run in fullscreen mode. 
	 * Jeremy '11,5,31
	 * Override fullscreen editing mode settings for larger screen  (>1.4in)
	 */

	@Override
	public boolean onEvaluateFullscreenMode() {
		DisplayMetrics dm = getResources().getDisplayMetrics();
		float displayHeight = dm.heightPixels;
		// If the display is more than X inches high, don't go to fullscreen mode
		float max = getResources().getDimension(R.dimen.max_height_for_fullscreen);
		if(DEBUG) 
			Log.i(TAG, "onEvaluateFullScreenMode() DisplayHeight:"+displayHeight+" limit:" + max 
					+ "super.onEvaluateFullscreenMode():" + super.onEvaluateFullscreenMode());
		//Jeremy '12,4,30 Turn off evaluation only for tablet and xhdpi phones (required horizontal >900pts)
		if (displayHeight > max && this.getMaxWidth() > 900) {  
			return false;
		} else {
			return super.onEvaluateFullscreenMode();
		}
	}

	/**
	 * This is called when the user is done editing a field. We can use this to
	 * reset our state.
	 */

	@Override
	public void onFinishInput() {
		
		if (DEBUG) {
			Log.i(TAG ,"onFinishInput()");
		}
		super.onFinishInput();

		if (mInputView != null) {
			mInputView.closing();
		}
		try {
		if(LDComposingBuffer.length()>0) { // Force interrupt the LD process
			LDComposingBuffer = "";
			SearchSrv.addLDPhrase(null,null,null,0, true);
		}
		// Jeremy '11,8,1 do postfinishinput in searchSrv (learn userdic and LDPhrase). 
		SearchSrv.postFinishInput();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		// Clear current composing text and candidates.
		//Jeremy '12,5,21 
		finishComposing();
		
		// -> 26.May.2011 by Art : Update keyboard list when user click the keyboard.
		try {
			mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
			mKeyboardSwitcher.setImList(SearchSrv.getImList());			
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		try{
			SearchSrv.close();
		}catch(Exception e){}
	}
	/**
	 * add by Jeremy '12,4,21
	 * Send ic.finishComposingText upon composing is about to end   
	 */
	private void finishComposing(){
		if(DEBUG)
			Log.i(TAG,"clearComposing()");
		//Jeremy '11,8,14
		if (mComposing != null && mComposing.length() > 0)
			mComposing.setLength(0);
		
		InputConnection ic = getCurrentInputConnection();
		if(ic!=null) ic.finishComposingText(); 
		
		firstMatched = null;
				
		//hasMappingList = false;
		if(templist!=null) 
			templist.clear();
		clearSuggestions();
	}
	/**
	 * add by Jeremy '12,4,21
	 * clearComposing buffer upon composing is about to end   
	 * add forceClearComposing parameter to control force clear the system composing buffer
	 * 
	 */
	/**
	 * @param forceClearComposing
	 */
	private void clearComposing(boolean forceClearComposing){
		if(DEBUG)
			Log.i(TAG,"clearComposing()");
		//Jeremy '11,8,14
		if (mComposing != null && mComposing.length() > 0)
			mComposing.setLength(0);
		
		
		if(forceClearComposing){
			InputConnection ic = getCurrentInputConnection();
			if(ic!=null) ic.commitText("", 0);
		}
		
		firstMatched = null;
				
		//hasMappingList = false;
		if(templist!=null) 
			templist.clear();
		clearSuggestions();
	}
	/**
	 * Clear suggestions or candidates in candidate view.
	 */
	private void clearSuggestions(){
		if(mCandidateView !=null){
			if(DEBUG) 
				Log.i(TAG, "clearSuggestions(): mInputView.isShown()" +mInputView.isShown()
					+ ", isCandidateShown():" + isCandidateShown());
			
			mCandidateView.clear();
			//hideCandidateView();
			if(!mEnglishOnly && mLIMEPref.getAutoChineseSymbol() //Jeremy '12,4,29 use mEnglishOnly instead of onIM 
					&& isCandidateShown())   //Jeremy '11,9,17 resolved the screen jumped complained by Julian 	
					//(mInputView.isShown()|| 
					 
				updateChineseSymbol(); // Jeremy '11,9,4
			else
				hideCandidateView();
				
		}
	}
	
	/**
	 * This is the main point where we do our initialization of the input method
	 * to begin operating on an application. At this point we have been bound to
	 * the client, and are now receiving all of the detailed information about
	 * the target of our edits.
	 */
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		if(DEBUG) 
			Log.i(TAG,"onStartInput()");
		super.onStartInputView(attribute, restarting);
		initOnStartInput(attribute, restarting);
	}

	@Override
	public void onStartInputView(EditorInfo attribute, boolean restarting) {
		if(DEBUG) 
			Log.i(TAG,"onStartInputView()");
		super.onStartInputView(attribute, restarting);
		initOnStartInput(attribute, restarting);
	}
	/**
	 * Initialization for IM and softkeybaords, and also choose wring lanaguage mode 
	 * according the input attrubute in editorInfo
	 */
	private void initOnStartInput(EditorInfo attribute, boolean restarting) {
	
		if (DEBUG)
			Log.i(TAG, "initOnStartInput");
		if (mInputView == null) {
			return;
		}
		
		isPhysicalKeyPressed = false;  //Jeremy '11,9,6 reset phsycalkeyflag
		// Reset the IM softkeyboard settings. Jeremy '11,6,19
		try {
			mKeyboardSwitcher.setImList(SearchSrv.getImList());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		// Reset templist
		this.firstMatched = null;
		//this.hasFirstMatched = false;
		if(templist != null){
			templist.clear();
		}
		
		mKeyboardSwitcher.makeKeyboards(false);

		//TextEntryState.newSession(this);
		loadSettings();
		// mImeOptions = attribute.imeOptions;
		mImeOptions = attribute.imeOptions;

		//initialKeyboard();
		buildActiveIMList();  //Jeremy '12,4,29 only this is required here instead of fully initialKeybaord
		//boolean disableAutoCorrect = false;
		mPredictionOn = true;
		mCompletionOn = false;
		mCompletions = null;
		mCapsLock = false;
		mHasShift = false;
		
		//isModeURL = false;
		//isModePassword = false;

		tempEnglishWord = new StringBuffer();
		tempEnglishList = new LinkedList<Mapping>();

		//onIM = true; //Jeremy '12,4,29 use mEnglishOnly instead of onIM

		switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
		case EditorInfo.TYPE_CLASS_NUMBER:
		case EditorInfo.TYPE_CLASS_DATETIME:
			mEnglishOnly = true;
			//onIM = false; //Jeremy '12,4,29 use mEnglishOnly instead of onIM
			mKeyboardSwitcher.setKeyboardMode(activeIMCode, LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, false, true, false);
			break;
		case EditorInfo.TYPE_CLASS_PHONE:
			mEnglishOnly = true;
			//onIM = false; //Jeremy '12,4,29 use mEnglishOnly instead of onIM
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
					LIMEKeyboardSwitcher.MODE_PHONE, mImeOptions, false, false, false);
			break;
		case EditorInfo.TYPE_CLASS_TEXT:

			// Make sure that passwords are not displayed in candidate view
			int variation = attribute.inputType
					& EditorInfo.TYPE_MASK_VARIATION;
			
			if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
					|| variation == EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME) {
				//mAutoSpace = false;
			} else {
				//mAutoSpace = true;
			}
			if (variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
				mPredictionOn = false;
			}
			if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT) == 0) {
				//disableAutoCorrect = true;
			}
			// If NO_SUGGESTIONS is set, don't do prediction.
			if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
				mPredictionOn = false;
				//disableAutoCorrect = true;
			}
			// If it's not multiline and the autoCorrect flag is not set, then
			// don't correct
			if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT) == 0
					&& (attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) == 0) {
				//disableAutoCorrect = true;
			}
			if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
				mPredictionOn = false;
				mCompletionOn = isFullscreenMode();
			}
			if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
					|| variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
				mPredictionOn = false;
				//isModePassword = true;
				mEnglishOnly = true;
				//onIM = false;//Jeremy '12,4,29 use mEnglishOnly instead of onIM
				mKeyboardSwitcher.setKeyboardMode(activeIMCode, LIMEKeyboardSwitcher.MODE_EMAIL,
									mImeOptions, false, false, false);
			} else if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
				mEnglishOnly = true;
				//onIM = false; //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				mPredictionOn = false;
				mKeyboardSwitcher.setKeyboardMode(activeIMCode,
						LIMEKeyboardSwitcher.MODE_EMAIL, mImeOptions, false, false, false);
			} else if (variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
				mPredictionOn = false;
				mEnglishOnly = true;
				//onIM = false; //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				//isModeURL = true;
				mKeyboardSwitcher.setKeyboardMode(activeIMCode,
						LIMEKeyboardSwitcher.MODE_URL, mImeOptions, false, false, false);
			} else if (variation == EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
				mEnglishOnly = false;
				mKeyboardSwitcher.setKeyboardMode(activeIMCode, LIMEKeyboardSwitcher.MODE_IM, mImeOptions, true, false, false);
			
			} else { 
				if(mPersistentLanguageMode)
					mEnglishOnly = mLIMEPref.getLanguageMode(); //Jeremy '12,4,30 restore lanaguage mode from preference.
				
				if(mPersistentLanguageMode && mEnglishOnly){
		        	mPredictionOn = true;
		        	mEnglishOnly = true;
			        //onIM = false; //Jeremy '12,4,29 use mEnglishOnly instead of onIM
			        mKeyboardSwitcher.setKeyboardMode(activeIMCode, LIMEKeyboardSwitcher.MODE_TEXT,	
			        		mImeOptions, false, false, false);
		        	
				} else{
					mEnglishOnly = false;
					//mKeyboardSwitcher.setKeyboardMode(keyboardSelection, LIMEKeyboardSwitcher.MODE_TEXT,	
			    	//	mImeOptions, true, false, false);
					initialIMKeyboard();  //'12,4,29 intial chinese IM keybaord
				}
			}

			// updateShiftKeyState(attribute);
			break;
		default:
			// mKeyboardSwitcher.setKeyboardMode(mKeyboardSwitcher.MODE_TEXT,
			// attribute.imeOptions);
			updateShiftKeyState(attribute);
		}

		mInputView.closing();
		
		mPredicting = false;
		//mDeleteCount = 0;

		/*
		 * // Override auto correct if (disableAutoCorrect) { mAutoCorrectOn =
		 * false; if (mCorrectionMode == Suggest.CORRECTION_FULL) {
		 * mCorrectionMode = Suggest.CORRECTION_BASIC; } }
		 * mInputView.setProximityCorrectionEnabled(true); if (mSuggest != null)
		 * { mSuggest.setCorrectionMode(mCorrectionMode); } mPredictionOn =
		 * mPredictionOn && mCorrectionMode > 0;
		 */
		updateShiftKeyState(getCurrentInputEditorInfo());
		
		//Jeremy '12,4,23  Force super to call onCreateCandidateView() so as composing popup won't fc. and will be hide in clearComposing
		showCandidateView();  
		clearComposing(false);
		forceHideCandidateView();//Force to hide the candidateview, we don't need it at this stage.
	
	}

	private void loadSettings() {
		
		hasVibration = mLIMEPref.getVibrateOnKeyPressed();
		hasSound = mLIMEPref.getSoundOnKeyPressed();
		mPersistentLanguageMode = mLIMEPref.getPersistentLanguageMode();
		activeIMCode = mLIMEPref.getActiveIM();
		hasQuickSwitch = mLIMEPref.getSwitchEnglishModeHotKey();
		mAutoCap = true; 
		
		disable_physical_selection = mLIMEPref.getDisablePhysicalSelkey();
		
		auto_commit = mLIMEPref.getAutoCommitValue();
		activeSoftKeyboard = mKeyboardSwitcher.getImKeyboard(activeIMCode);
		
		//mQuickFixes = true;
		// If there is no auto text data, then quickfix is forced to "on", so
		// that the other options
		// will continue to work
		//if (AutoText.getSize(mInputView) < 1)
			//mQuickFixes = true;
		//mShowSuggestions = true & mQuickFixes;// sp.getBoolean(PREF_SHOW_SUGGESTIONS,
		// true) & mQuickFixes;
		//boolean autoComplete = true;// sp.getBoolean(PREF_AUTO_COMPLETE,
		// getResources().getBoolean(R.bool.enable_autocorrect)) &
		// mShowSuggestions;
		//mAutoCorrectOn = mSuggest != null && (autoComplete || mQuickFixes);
		//mCorrectionMode = autoComplete ? Suggest.CORRECTION_FULL
		//		: (mQuickFixes ? Suggest.CORRECTION_BASIC
		//				: Suggest.CORRECTION_NONE);
	}

	/**
	 * Deal with the editor reporting movement of its cursor.
	 */
	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart,
			int candidatesEnd) {
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
				candidatesStart, candidatesEnd);
		
		if(DEBUG) 
			Log.i(TAG, "onUpdateSelection():oldSelStart" + oldSelStart
					+" oldSelEnd:" + oldSelEnd
					+" newSelStart:" + newSelStart + " newSelEnd:" + newSelEnd
					+" candidatesStart:"+candidatesStart + " candidatesEnd:"+candidatesEnd	);

		// If the current selection in the text view changes, we should
		// clear whatever candidate text we have 
		// (in English prediction do nothing in chinese IM). 
		// Jeremy '12,4,9  do nothing here now.
		
		if (mComposing.length() > 0
				&& candidatesStart >0 && candidatesEnd >0 // in composing  
				&& (newSelStart != candidatesEnd || newSelEnd != candidatesEnd) // cursor is not in the last character of composing area 
				) {
			if(newSelStart < candidatesStart || newSelStart > candidatesEnd) { // cursor is moved before or after composing area

				if(templist!=null) 	templist.clear();
				mCandidateView.clear();
				hideCandidateView();

				if (mComposing != null && mComposing.length() > 0){

					mComposing.setLength(0);

					InputConnection ic = getCurrentInputConnection();
					if (ic != null) 
						ic.finishComposingText();
				}
			}else{
				// cursor is moved within the composing area by user. move the cursor back to the end of composing area (don't know how to do now)
			}
			
		}
		
		
	}

	/**
	 * This tells us about completions that the editor has determined based on
	 * the current text in it. We want to use this in fullscreen mode to show
	 * the completions ourself, since the editor can not be seen in that
	 * situation.
	 */
	@Override
	public void onDisplayCompletions(CompletionInfo[] completions) {
		if (DEBUG)
			Log.i(TAG, "onDisplayCompletions()");
		if (mCompletionOn){
			mCompletions = completions;
			if(!mEnglishOnly){ //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				if(mComposing.length()==0) updateRelatedWord();
			} 
			if(mEnglishOnly && !mPredictionOn) {
				setSuggestions(buildCompletionList(), false, true,"");
			}
				
		}
	}

	/**
	 * This translates incoming hard key events in to edit operations on an
	 * InputConnection. It is only needed when using the PROCESS_HARD_KEYS
	 * option.
	 */
	private boolean translateKeyDown(int keyCode, KeyEvent event) {
		// move to HandleCharacter '10, 3,26
		// mMetaState = LIMEMetaKeyKeyListener.handleKeyDown(mMetaState,
		// keyCode, event);
		// mMetaState =
		// LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
		isPhysicalKeyPressed = true; // Jeremy '11,9,5
		
		int c = event.getUnicodeChar(LIMEMetaKeyKeyListener
				.getMetaState(mMetaState));

		InputConnection ic = getCurrentInputConnection();
		
		/// Jeremy '12,4,1 XPERIA Pro force translating special keys 
		if(mLIMEPref.getPhysicalKeyboardType().equals("xperiapro")) {
			boolean isShift = LIMEMetaKeyKeyListener.getMetaState(mMetaState,
					LIMEMetaKeyKeyListener.META_SHIFT_ON) > 0;
			switch(keyCode){	
			case KeyEvent.KEYCODE_AT:
				if(isShift) c ='/';
				else c = '!';
				break;
			case KeyEvent.KEYCODE_APOSTROPHE:
				if(isShift) c ='"';
				else c = '\'';
				break;
			case KeyEvent.KEYCODE_GRAVE:
				if(isShift) c ='~';
				else c = '`';
				break;
			case KeyEvent.KEYCODE_COMMA:
				if(isShift) c ='?';
				else c = '.';
				break;
			case KeyEvent.KEYCODE_PERIOD:
				if(isShift) c ='>';
				else c = '@';
				break;
			
			}
		}

		if (c == 0 || ic == null) {
			return false;
		}

		// Compact code by Jeremy '10, 3, 27
		if (keyCode == 59) { // Translate shift as -1
			c = -1;
		}
		if (c != -1 && (c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
			c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
		}
		onKey(c, null);
		return true;
	}

	

	//private boolean hasEscPressStep1 = false;
	//private boolean hasEscPressStep2 = false;

	/**
	 * Physical KeyBoard Event Handler Use this to monitor key events being
	 * delivered to the application. We get first crack at them, and can either
	 * resume them or let them continue to the app.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Clean code by jeremy '11,8,22
		if (DEBUG) 
			Log.i(TAG, "OnKeyDown():keyCode:" + keyCode 
					+ ", event.getDownTime()"+ event.getDownTime() 
					+ ", event.getEventTime()"+ event.getEventTime()
					+ ", event.getRepeatCount()" + event.getRepeatCount());
		
		// Force To Close VKeyboard
		if(mInputView.isShown()){
			mInputView.closing();
		}
		
		/*if(!(keyCode == KeyEvent.KEYCODE_HOME
			 ||keyCode == KeyEvent.KEYCODE_BACK
			 ||keyCode == KeyEvent.KEYCODE_MENU
			 ||keyCode == KeyEvent.KEYCODE_SEARCH
			 )) //Jeremy '11,9,4 exclude the four default hard keys
				isPhysicalKeyPressed = true;*/ // Moved to translatekeydown '11,9,5 by jeremy

		mKeydownEvent = new KeyEvent(event);
		// Record key pressed time and set key processed flags(key down, for physical keys)
		//Jeremy '11,8,22 using getRepeatCount from event to set processed flags
		if (event.getRepeatCount()==0){//!keydown) {
			//keyPressTime = System.currentTimeMillis();
			//keydown = true;
			hasKeyProcessed = false;
			hasMenuProcessed = false; // only do this on first keydown event
			hasEnterProcessed = false;
			hasSpaceProcessed = false;
			hasSymbolEntered = false;
		}
		
		switch (keyCode) {
		// Jeremy '11,5,29 Bypass search and menu combination keys.
		case KeyEvent.KEYCODE_MENU:
			
			hasMenuPress = true;
			break;
		// Add by Jeremy '10, 3, 29. DPAD selection on candidate view
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			// Log.i("ART","select:"+1);
			if (mCandidateView != null && isCandidateShown()) {
				mCandidateView.selectNext();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			// Log.i("ART","select:"+2);
			if (mCandidateView != null && isCandidateShown()) {
				mCandidateView.selectPrev();
				return true;
			}
			break;
		//Jeremy '11,8,28 for expanded canddiateviewi
		case KeyEvent.KEYCODE_DPAD_UP:
			// Log.i("ART","select:"+2);
			if (mCandidateView != null && isCandidateShown()) {
				mCandidateView.selectPrevRow();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			// Log.i("ART","select:"+2);
			if (mCandidateView != null && isCandidateShown()) {
				mCandidateView.selectNextRow();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			// Log.i("ART","select:"+3);
			if (mCandidateView != null && isCandidateShown()) {
				mCandidateView.takeSelectedSuggestion();
				return true;
			}
			break;
		// Add by Jeremy '10,3,26, process metakey with
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			hasShiftPress = true;
			mMetaState = LIMEMetaKeyKeyListener.handleKeyDown(mMetaState,
					keyCode, event);
			break;
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			mMetaState = LIMEMetaKeyKeyListener.handleKeyDown(mMetaState,
					keyCode, event);
			break;
		case MY_KEYCODE_CTRL_LEFT:
		case MY_KEYCODE_CTRL_RIGHT:
			hasCtrlPress = true;
			break;
		case MY_KEYCODE_WINDOWS_START:
			hasWinPress = true;
			break;
		case MY_KEYCODE_ESC:
		case KeyEvent.KEYCODE_BACK:
			// The InputMethodService already takes care of the back
			// key for us, to dismiss the input method if it is shown.
			// However, our keyboard could be showing a pop-up window
			// that back should dismiss, so we first allow it to do that.
			
			if (event.getRepeatCount() == 0) {
				if(mInputView != null && mInputView.handleBack()){
					Log.i(TAG,"KEYCODE_BACK mInputView handled the backed key");
					return true;
				}
				//Jeremy '12,4,8 rewrite the logic here
				else if(!mEnglishOnly && mCandidateView !=null && isCandidateShown() //Jeremy '12,4,29 use mEnglishOnly instead of onIM
						&& ( mComposing.length() > 0 || 
						(firstMatched != null && firstMatched.isDictionary() &&	!isChineseSymbolSuggestionsShowing )
						) ){
					if(DEBUG)
						Log.i(TAG,"KEYCODE_BACK clearcomposing only.");
					//Jeremy 12,4,21 -- need to check again
					finishComposing();
					return true;
				}else {
					super.setCandidatesViewShown(false);
					if(DEBUG)
						Log.i(TAG,"KEYCODE_BACK return to super.");
				}
					
			}
			break;

		case KeyEvent.KEYCODE_DEL:
			// Special handling of the delete key: if we currently are
			// composing text for the user, we want to modify that instead
			// of let the application to the delete itself.

			//if (mComposing.length() > 0 || tempEnglishWord.length() > 0 
			//	||(mCandidateView!=null&&isCandidateShown())){ //Jeremy '11,9,10 
			onKey(LIMEBaseKeyboard.KEYCODE_DELETE, null);
			return true;
			
			/*} else {
				if (mComposing.length() > 0) {
					onKey(LIMEBaseKeyboard.KEYCODE_DELETE, null);
					return true;
				}*/
			

			//clearComposing();
			//mMetaState = LIMEMetaKeyKeyListener
			//		.adjustMetaAfterKeypress(mMetaState);
			//setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState();
			// ------------------------------------------------------------------------

			//break;

		case KeyEvent.KEYCODE_ENTER:
			// Let the underlying text editor always handle these, if return
			// false from takeSelectedSuggestion().
			// Process enter for candidate view selection in OnKeyUp() to block
			// the real enter afterward.
			// return false;
			// Log.i("ART", "physical keyboard:"+ keyCode);
			if(!mEnglishOnly){ //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				if (mCandidateView != null && isCandidateShown()) {
				// To block a real enter after suggestion selection. We have to
				// return true in OnKeyUp();					
					if( mCandidateView.takeSelectedSuggestion()){
						hasEnterProcessed = true;
						return true;
					}else{
						hideCandidateView();
						break;
					}
				}
			}else{
				if (mLIMEPref.getEnglishPrediction() && mPredictionOn
						&& ( mLIMEPref.getEnglishPredictionOnPhysicalKeyboard())){
					resetTempEnglishWord();
					this.updateEnglishPrediction();
				}
			}
			break;

/*		case MY_KEYCODE_ESC:
		//Jeremy '11,9,7 treat esc as back key
			//Jeremy '11,8,14
			clearComposing();
			InputConnection ic=getCurrentInputConnection();
			if(ic!=null) ic.commitText("", 0);
			return true;*/

		case KeyEvent.KEYCODE_SPACE:

		
			hasQuickSwitch = mLIMEPref.getSwitchEnglishModeHotKey();

			// If user enable Quick Switch Mode control then check if has
			// 	Shift+Space combination
			// '11,5,13 Jeremy added Ctrl-space switch chi/eng
			// '11,6,18 Jeremy moved from on_KEY_UP
			// '12,4,29 Heremy add hasWinPress + space to switch chi/eng 

			if ((hasQuickSwitch && hasShiftPress) || hasCtrlPress || hasMenuPress || hasWinPress) { 
				this.switchChiEng();
				if(hasMenuPress)  hasMenuProcessed = true;
				hasSpaceProcessed =true;
				
				return true;
			} else {
				if (!mEnglishOnly) { // //Jeremy '12,4,29 use mEnglishOnly instead of onIM
					
					if (mCandidateView != null && isCandidateShown()) {
						if (mCandidateView.takeSelectedSuggestion()) {
							return true;
						} else {
							hideCandidateView();
							break;
						}
					} 
				} else {
					//if (tempEnglishList != null && tempEnglishList.size() > 1	&& tempEnglishWord != null && tempEnglishWord.length() > 0) {
					if(mLIMEPref.getEnglishPrediction()){
						resetTempEnglishWord();
					}
					this.updateEnglishPrediction();
				}
				break;
			}
		case MY_KEYCODE_SWITCH_CHARSET: // experia pro earth key
		case 1000: // milestone chi/eng key
			switchChiEng();
			break;
		case KeyEvent.KEYCODE_SYM:	
		case KeyEvent.KEYCODE_AT:		
			//Jeremy '11,8,22 use begintime and eventtime in event to see if long-pressed or not.
			if (//keyPressTime != 0 && 
					!hasKeyProcessed 
					&& event.getRepeatCount() > 0
					&& event.getEventTime() -  event.getDownTime() > mLongPressKeyTimeout ) {
					//&& System.currentTimeMillis() - keyPressTime > mLongPressKeyTimeout){
				switchChiEng();
				hasKeyProcessed = true;
			}
			return true;
		case KeyEvent.KEYCODE_TAB: // Jeremy '11,5,23: Force bypassing tab
									// processing to super
			break;
		default:
			
			//if(hasSearchPress) hasSearchProcessed = true;
			if(!(hasCtrlPress||hasMenuPress)){
				if (translateKeyDown(keyCode, event)) {
					 if(DEBUG) Log.i(TAG,"Onkeydown():tranlatekeydown:true");
					return true;
				}
			}

		}
		 
		int primaryKey = event.getUnicodeChar(LIMEMetaKeyKeyListener.getMetaState(mMetaState));
		char t = (char) primaryKey;

		if((hasCtrlPress||hasMenuPress)&& !mEnglishOnly ) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
			
			if (hasCtrlPress &&  //Only working with ctrl Jeremy '11,8,22
				templist != null && templist.size() > 0 
				&& mCandidateView != null && isCandidateShown()){	
				switch(keyCode){
				case 8: this.pickSuggestionManually(0);return true;
				case 9: this.pickSuggestionManually(1);return true;
				case 10: this.pickSuggestionManually(2);return true;
				case 11: this.pickSuggestionManually(3);return true;
				case 12: this.pickSuggestionManually(4);return true;
				case 13: this.pickSuggestionManually(5);return true;
				case 14: this.pickSuggestionManually(6);return true;
				case 15: this.pickSuggestionManually(7);return true;
				case 16: this.pickSuggestionManually(8);return true;
				case 7: this.pickSuggestionManually(9);return true;
				}
			}
			if((mComposing == null || mComposing.length() == 0) ) {		
				// Jeremy '11,8,21.  Ctrl-/ to fetch full-shaped chinese symbols in candidateview.
				if(t=='/'){
					if(hasMenuPress) hasMenuProcessed = true;
					updateChineseSymbol();
					return true;
				}
				// 27.May.2011 Art : when user click Ctrl + Symbol or number then send Chinese Symobl Characters
				String s = ChineseSymbol.getSymbol(t);
				if(s != null){
					clearSuggestions();
					getCurrentInputConnection().commitText(s, 0);
					hasSymbolEntered = true;
					if(hasMenuPress) hasMenuProcessed = true;
					return true;

				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	/*  deprecated by jeremy '12,4,29
	private void resetCandidateBar() {
		//Mapping empty = new Mapping();
		//empty.setWord("");
		//empty.setDictionary(true);

		if(!hasCtrlPress){
			//LinkedList<Mapping> list = new LinkedList<Mapping>();
			//list.add(empty);
			//Jermy '11,8,14
			clearSuggestions();
		}
	}
	*/
	private void resetTempEnglishWord() {
		tempEnglishWord.delete(0, tempEnglishWord.length());
		tempEnglishList.clear();
	}

	private void setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState() {
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
			int clearStatesFlags = 0;
			if (LIMEMetaKeyKeyListener.getMetaState(mMetaState,
					LIMEMetaKeyKeyListener.META_ALT_ON) == 0)
				clearStatesFlags += KeyEvent.META_ALT_ON;
			if (LIMEMetaKeyKeyListener.getMetaState(mMetaState,
					LIMEMetaKeyKeyListener.META_SHIFT_ON) == 0)
				clearStatesFlags += KeyEvent.META_SHIFT_ON;
			if (LIMEMetaKeyKeyListener.getMetaState(mMetaState,
					LIMEMetaKeyKeyListener.META_SYM_ON) == 0)
				clearStatesFlags += KeyEvent.META_SYM_ON;
			ic.clearMetaKeyStates(clearStatesFlags);
		}
	}

	/**
	 * Use this to monitor key events being delivered to the application. We get
	 * first crack at them, and can either resume them or let them continue to
	 * the app.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (DEBUG) {
			Log.i(TAG,"OnKeyUp():keyCode:" + keyCode + ";hasCtrlPress:"
					+ hasCtrlPress
			/*
			 * + " KeyEvent.Alt_ON:" +
			 * String.valueOf(LIMEMetaKeyKeyListener.getMetaState( mMetaState,
			 * LIMEMetaKeyKeyListener.META_ALT_ON)) + " KeyEvent.Shift_ON:" +
			 * String.valueOf(LIMEMetaKeyKeyListener.getMetaState( mMetaState,
			 * LIMEMetaKeyKeyListener.META_SHIFT_ON))
			 */
			);

		}
		//keydown = false;
	

		switch (keyCode) {
		//Jeremy '11,5,29 Bypass search and menu keys.
//		case KeyEvent.KEYCODE_SEARCH:
//			hasSearchPress = false;
//			if(hasSearchProcessed) return true;
//			break;
		case KeyEvent.KEYCODE_MENU:
			hasMenuPress = false;
			if(hasMenuProcessed) return true;
			break;
		// */------------------------------------------------------------------------
		// Modified by Jeremy '10, 3,12
		// keep track of alt state with mHasAlt.
		// Modified '10, 3, 24 for bug fix and alt-lock implementation
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			hasShiftPress = false;
			mMetaState = LIMEMetaKeyKeyListener.handleKeyUp(mMetaState,	keyCode, event);
			// '11,8,28 Jeremy popup keyboard picker instaead of nextIM when onIM
			// '11,5,14 Jeremy ctrl-shift switch to next available keyboard; 
			// '11,5,24 blocking switching if full-shape symbol 
			if (!hasSymbolEntered && !mEnglishOnly && (hasMenuPress || hasCtrlPress) ){ //Jeremy '12,4,29 use mEnglishOnly instead of onIM  
				//nextActiveKeyboard(true);
				showIMPicker(); //Jeremy '11,8,28
				if(hasMenuPress) {
					hasMenuProcessed = true;
					hasMenuPress = false;
				}
				mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			mMetaState = LIMEMetaKeyKeyListener.handleKeyUp(mMetaState,
					keyCode, event);
			break;
		case MY_KEYCODE_CTRL_LEFT:
		case MY_KEYCODE_CTRL_RIGHT:
			hasCtrlPress = false;
			break;
		case MY_KEYCODE_WINDOWS_START:
			hasWinPress = false;
			break;
		case KeyEvent.KEYCODE_ENTER:
			// Add by Jeremy '10, 3 ,29. Pick selected selection if candidates
			// shown.
			// Does not block real enter after select the suggestion. !! need
			// fix here!!
			// Let the underlying text editor always handle these, if return
			// false from takeSelectedSuggestion().
			// if (mCandidateView != null && isCandidateShown()) {
			// return mCandidateView.takeSelectedSuggestion();
			// }
			// Log.i("ART", "physical keyboard onkeyup:"+ keyCode);
			if (hasEnterProcessed) {
				return true;
			}
			// Jeremy '10, 4, 12 bug fix on repeated enter.
			break;
		//Jeremy '11,8,14
		//case KeyEvent.KEYCODE_SEARCH:
		/*case KeyEvent.KEYCODE_PERIOD:
			if (hasKeyProcessed){ //keyPressTime != 0
					//&& System.currentTimeMillis() - keyPressTime > 700) {
				//updateChineseSymbol();  // Jeremy '11,8,15 moved to onKeyDown()
				return true;
			} else if (((mEnglishOnly && mPredictionOn) || (!mEnglishOnly && onIM))
					&& translateKeyDown(keyCode, event)) {
				return true;
			} else {
				translateKeyDown(keyCode, event);
				super.onKeyDown(keyCode, mKeydownEvent);
			}
			break;*/
		case KeyEvent.KEYCODE_SYM:
		case KeyEvent.KEYCODE_AT:
			if(hasKeyProcessed){  //(keyPressTime != 0
				//&& System.currentTimeMillis() - keyPressTime > 700) {
				//switchChiEng(); // Jeremy '11,8,15 moved to onKeyDown()
				return true;
			} else if (LIMEMetaKeyKeyListener.getMetaState(mMetaState,
					LIMEMetaKeyKeyListener.META_SHIFT_ON) > 0 && !mEnglishOnly //Jeremy '12,4,29 use mEnglishOnly instead of onIM
					&& !mLIMEPref.getPhysicalKeyboardType().equals("xperiapro")) {  // '12,4,1 Jeremy XPERIA Pro does not use this key as @
				// alt-@ is conflict with symbol input thus altered to shift-@ Jeremy '11,8,15
				// alt-@ switch to next active keyboard.
				//nextActiveKeyboard(true);
				showIMPicker(); //Jeremy '11,8,28
				mMetaState = LIMEMetaKeyKeyListener
						.adjustMetaAfterKeypress(mMetaState);
				setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState(); 
				return true;
				// Long press physical @ key to swtich chn/eng 
			} else if (((mEnglishOnly && mPredictionOn) || (!mEnglishOnly))
					&& translateKeyDown(keyCode, event)) {
				return true;
			} else {
				translateKeyDown(keyCode, event);
				super.onKeyDown(keyCode, mKeydownEvent);
			}

			break;
			
		case KeyEvent.KEYCODE_SPACE:
			//Jeremy move the chi/eng swithcing to on_KEY_UP '11,6,18
			if(hasSpaceProcessed)
				return true;
		default:

		}
		// Update metakeystate of IC maintained by MetaKeyKeyListerner
		setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState();

		
		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Helper function to commit any text being composed in to the editor.
	 */
	private void commitTyped(InputConnection ic) {
		if (DEBUG)
			Log.i(TAG,"CommittedTyped()");
		try {
			if (mComposing.length() > 0
					|| (firstMatched != null && firstMatched.isDictionary())) {

				if (!mEnglishOnly) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
					if (firstMatched != null && firstMatched.getWord() != null
							&& !firstMatched.getWord().equals("")) {
						/*int firstMatchedLength = firstMatched.getWord()
								.length();*/
						int firstMatchedLength = 1;

						if (firstMatched.getCode() == null
								|| firstMatched.getCode().equals("")) {
							firstMatchedLength = 1;
						}

						String wordToCommit = firstMatched.getWord();

						if (firstMatched != null
								&& firstMatched.getCode() != null
								&& firstMatched.getWord() != null) {
							if (firstMatched
									.getCode()
									.toLowerCase()
									.equals(firstMatched.getWord()
											.toLowerCase())) {
								firstMatchedLength = 1;

								// if end with code then append " " space
								// wordToCommit += " ";
							}
						}

						if (DEBUG)
							Log.i(TAG,"CommitedTyped() commited Length="
									+ firstMatchedLength);
						// Do hanConvert before commit
						// '10, 4, 17 Jeremy
						// inputConnection.setComposingText("", 1);
						if(ic!=null) ic.commitText(
								SearchSrv.hanConvert(wordToCommit),
								firstMatchedLength);

						try {
							SearchSrv.addUserDict(firstMatched.getId(),
									firstMatched.getCode(),
									firstMatched.getWord(),
									firstMatched.getPword(),
									firstMatched.getScore(),
									firstMatched.isDictionary());
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						//userdiclist.add(firstMatched);
						// Update userdict for auto-learning feature
						// if(userdiclist.size() > 1) { updateUserDict();}

						// Add by Jeremy '10, 4,1 . Reverse Lookup
						SearchSrv.rQuery(firstMatched.getWord());
						
						// Art '30,Sep,2011 when show related then clear composing
						if(activeSoftKeyboard.indexOf("wb") != -1){
							clearComposing(true);
						}
						
						// Jeremy '11,7,28 for continuous typing (LD) 
						boolean composingNotFinish = false;
						//String commitedCode = firstMatched.getCode();
						int commitedCodeLength=firstMatched.getCode().length();
						if(activeIMCode.equals("phonetic") &&
								mComposing.length() >= firstMatched.getCode().length()){
								String strippedCode = firstMatched.getCode().trim().replaceAll("[3467]", "");
								//commitedCode = strippedCode;
							if(mComposing.toString().contains(firstMatched.getCode())){
								if(mComposing.length() > firstMatched.getCode().length())
									composingNotFinish = true;
							}else if(mComposing.toString().contains(strippedCode)){
								composingNotFinish = true;
								commitedCodeLength = strippedCode.length();
							}
							
						}else if(mComposing.length() > firstMatched.getCode().length()){
							composingNotFinish = true;
						}
						
						if(composingNotFinish){
							if(LDComposingBuffer.length()==0){
								//starting LD process
								LDComposingBuffer = mComposing.toString();
								if(DEBUG) 
									Log.i(TAG, "commitedtype():starting LD process, LDBuffer=" + LDComposingBuffer +
										". just commited code=" + firstMatched.getCode());
								SearchSrv.addLDPhrase(firstMatched.getId(), firstMatched.getCode(), 
										firstMatched.getWord(), firstMatched.getScore(), false);
							}else if(LDComposingBuffer.contains(mComposing.toString())){
								//Continuous LD process
								if(DEBUG) 
									Log.i(TAG, "commitedtype():Continuous LD process, LDBuffer=" + LDComposingBuffer +
										". just commited code=" + firstMatched.getCode());
								SearchSrv.addLDPhrase(firstMatched.getId(), firstMatched.getCode(), 
										firstMatched.getWord(), firstMatched.getScore(), false);
							}
							mComposing= mComposing.delete(0, commitedCodeLength);
							
							if(!mComposing.toString().equals(" ")){
								if(mComposing.toString().startsWith(" "))
									mComposing= mComposing.deleteCharAt(0);
								if(DEBUG) Log.i(TAG, "commitedtype(): new mComposing:" +mComposing);
								if(ic!=null) ic.setComposingText(mComposing, 1);
								updateCandidates();
								return;
							}
						} else {
							if(LDComposingBuffer.length()>0 && LDComposingBuffer.contains(mComposing.toString())){
								//Ending continuous LD process (last of LD process)
								if(DEBUG) 
									Log.i(TAG, "commitedtype():Ending LD process, LDBuffer=" + LDComposingBuffer +
										". just commited code=" + firstMatched.getCode());
								LDComposingBuffer = "";
								SearchSrv.addLDPhrase(firstMatched.getId(), firstMatched.getCode(), firstMatched.getWord(), firstMatched.getScore(), true);
							}else if(LDComposingBuffer.length()>0){
								//LD process interrupted.
								if(DEBUG) 
									Log.i(TAG, "commitedtype():LD process interrupted, LDBuffer=" + LDComposingBuffer +
										". just commited code=" + firstMatched.getCode());
								LDComposingBuffer = "";
								SearchSrv.addLDPhrase(null,null,null,0, true);
							}
								
						}

						tempMatched = firstMatched;
						firstMatched = null;
						//hasFirstMatched = true;
					} else if (firstMatched != null
							&& firstMatched.getWord() != null
							&& firstMatched.getWord().equals("")) {
						if(ic!=null) ic.commitText(misMatched,
								misMatched.length());
						firstMatched = null;
						//hasFirstMatched = false;

						//userdiclist.add(null);
					} else {
						if(ic!=null) ic.commitText(mComposing,
								mComposing.length());
						//hasFirstMatched = false;
						//userdiclist.add(null);
					}
				} else {
					if(ic!=null) ic.commitText(mComposing, mComposing.length());
					//hasFirstMatched = false;
					//userdiclist.add(null);
				}

				
				finishComposing();				//Jeremy '12, 4 ,21
				if(!mEnglishOnly) //Jeremy '12,4,29 use mEnglishOnly instead of onIM
					updateRelatedWord();

				

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Jeremy '11,8,1 deprecated
/*	private void updateUserDict() {
 //Jeremy '11,7,27 Using signle list for addscore and adduserdict.
		for (Mapping dicunit : userdiclist) {
			if (dicunit == null) {
				continue;
			}
			//if (dicunit.getId() == null) {
			//	continue;
			//}
			if (dicunit.getCode() == null) {
				continue;
			}
			try {
				SearchSrv.addUserDict(dicunit.getId(), dicunit.getCode(),
						dicunit.getWord(), dicunit.getPword(),
						dicunit.getScore(), dicunit.isDictionary());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		try {
			SearchSrv.postFinishInput();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		//userdiclist.clear();
	}
*/
	/**
	 * Helper to update the shift state of our keyboard based on the initial
	 * editor state.
	 */
	public void updateShiftKeyState(EditorInfo attr) {
		if(DEBUG) Log.i(TAG, "updateShiftKeyState()");
		InputConnection ic = getCurrentInputConnection();
		if (attr != null && mInputView != null
				&& mKeyboardSwitcher.isAlphabetMode() && ic != null) {
			int caps = 0;
			EditorInfo ei = getCurrentInputEditorInfo();
			if (mAutoCap && ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
				caps = ic.getCursorCapsMode(attr.inputType);
			}
			mInputView.setShifted(mCapsLock || caps != 0);
		} else {
			if (!mCapsLock && mHasShift) {
				mKeyboardSwitcher.toggleShift();
				mHasShift = false;
			}
		}

	}

	private boolean isValidLetter(int code) {
		if (Character.isLetter(code)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isValidDigit(int code) {
		if (Character.isDigit(code)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isValidSymbol(int code) {
		String checkCode = String.valueOf((char) code);
		// code has to < 256, a ascii character
		if (code < 256 && checkCode.matches(".*?[^A-Z]")
				&& checkCode.matches(".*?[^a-z]")
				&& checkCode.matches(".*?[^0-9]") && code != 32) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Helper to send a key down / key up pair to the current editor.
	 */
	private void keyDownUp(int keyEventCode) {
		InputConnection ic=getCurrentInputConnection();
		if(ic!=null){
			ic.sendKeyEvent(
					new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
			ic.sendKeyEvent(
					new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
		}
	}

	/**
	 * Helper to send a character to the editor as raw key events.
	 *
	private void sendKey(int keyCode) {

		switch (keyCode) {
		case '\n':
			keyDownUp(KeyEvent.KEYCODE_ENTER);
			break;
		default:

			if (keyCode == 32 && firstMatched == null && !hasFirstMatched) {
				getCurrentInputConnection().commitText(
						String.valueOf((char) keyCode), 1);
			} else {
				if (keyCode != 32 && firstMatched != null && !hasFirstMatched) {
					getCurrentInputConnection().commitText(
							String.valueOf((char) keyCode), 1);
				} else if (keyCode != 32) {
					getCurrentInputConnection().commitText(
							String.valueOf((char) keyCode), 1);
				} else if (keyCode == 32 && (!hasFirstMatched)) {
					getCurrentInputConnection().commitText(
							String.valueOf((char) keyCode), 1);
				} else if (keyCode == 32 && this.mComposing.length() == 0
						&& this.tempMatched != null
						&& !this.tempMatched.getCode().trim().equals("")) {
					// Press Space Button + has matched keyword then do nothing but pump related candidates
					//Jeremy '11,5,31 commitTyped does not pump related candidates now do here. 
					updateDictionaryView();
				} else if (keyCode == 32 && this.mComposing.length() == 0
						&& this.tempMatched != null
						&& this.tempMatched.getCode().trim().equals("")) {
					// Press Space Button + no matched keyword consider as
					// English append space at the end
					getCurrentInputConnection().commitText(
							String.valueOf((char) keyCode), 1);
				}
				hasFirstMatched = false;
			}
			break;
		}
	}
*/
	public void onKey(int primaryCode, int[] keyCodes) {
		onKey(primaryCode, keyCodes,0,0);
	}
	public void onKey(int primaryCode, int[] keyCodes, int x, int y) {
		if (DEBUG) 
			Log.i(TAG, "OnKey(): primaryCode:" + primaryCode
					+ " hasShiftPress:" + hasShiftPress);
		
		if (mLIMEPref.getEnglishPrediction()
				&& primaryCode != LIMEBaseKeyboard.KEYCODE_DELETE) {
			

			// Chcek if input character not valid English Character then reset
			// temp english string
			if (!Character.isLetter(primaryCode) && mEnglishOnly) {
				
				//Jeremy '11,6,10. Select english sugestion with shift+123457890
				if (isPhysicalKeyPressed &&(mCandidateView != null && isCandidateShown()) ){
					if(handleSelkey(primaryCode, keyCodes))		{
						return;
					}
					resetTempEnglishWord();
					if(!hasCtrlPress) clearSuggestions(); //Jeremy '12,4,29 moved from resetcandidateBar
				}
				
			}
		}

		// Handle English/Lime Keyboard switch
		if (mEnglishFlagShift == false
				&& (primaryCode == LIMEBaseKeyboard.KEYCODE_SHIFT)) {
			mEnglishFlagShift = true;
			if (DEBUG) {
				Log.i(TAG, "OnKey():mEnglishFlagShift:" + mEnglishFlagShift);
			}
		}
		if (primaryCode == LIMEBaseKeyboard.KEYCODE_DELETE) {
			handleBackspace();
		} else if (primaryCode == LIMEBaseKeyboard.KEYCODE_SHIFT) {
			if (DEBUG) 	Log.i(TAG, "OnKey():KEYCODE_SHIFT");
			if(!(!isPhysicalKeyPressed && hasDistinctMultitouch))
				handleShift();
		} else if (primaryCode == LIMEBaseKeyboard.KEYCODE_CANCEL) {
			handleClose();
			return;
			// long press on options and shift
		} else if (primaryCode == LIMEKeyboardView.KEYCODE_OPTIONS) {
			handleOptions();
		} else if( primaryCode == LIMEKeyboardView.KEYCODE_SPACE_LONGPRESS) {
			showIMPicker();
		} else if (primaryCode == LIMEBaseKeyboard.KEYCODE_MODE_CHANGE	&& mInputView != null) {
			switchKeyboard(primaryCode);
		} else if (primaryCode == LIMEKeyboardView.KEYCODE_NEXT_IM){
			nextActiveIM(true);
		} else if (primaryCode == LIMEKeyboardView.KEYCODE_PREV_IM){
			nextActiveIM(false);
		} else if (primaryCode == KEYBOARD_SWITCH_CODE && mInputView != null) { //chi->eng
			//mEnglishOnly = true;
			//onIM = false;
			switchKeyboard(primaryCode);
			// Jeremy '11,5,31 Rewrite softkeybaord enter/space and english sepeartor processing.
		} else if (primaryCode == KEYBOARD_SWITCH_IM_CODE && mInputView != null) { //eng -> chi
			switchKeyboard(primaryCode);
		} else if (!mEnglishOnly && //Jeremy '12,4,29 use mEnglishOnly instead of onIM  
				((primaryCode== MY_KEYCODE_SPACE && !activeIMCode.equals("phonetic"))
				||(primaryCode== MY_KEYCODE_SPACE && 
						activeIMCode.equals("phonetic") && !mLIMEPref.getParameterBoolean("doLDPhonetic", false) )
				||(primaryCode== MY_KEYCODE_SPACE && 
						activeIMCode.equals("phonetic") && (mComposing.toString().endsWith(" ")|| mComposing.length()==0 ))
				|| primaryCode == MY_KEYCODE_ENTER) ){
			
			if ( mCandidateView != null && isCandidateShown()){ 
				boolean nullComposing = false;
				if(mComposing.length() == 0){
					nullComposing = true;
				}
				if(!mCandidateView.takeSelectedSuggestion()){
					hideCandidateView();
					sendKeyChar((char)primaryCode);
				}else if(nullComposing){
					sendKeyChar((char)primaryCode);
				}
				
			}else{
				 sendKeyChar((char)primaryCode);
			}
		} else if (mEnglishOnly && isWordSeparator(primaryCode)) {
            handleSeparator(primaryCode); 
            
		} else {

			handleCharacter(primaryCode, keyCodes);
			
			// Art 11, 9, 26 Check if need to auto commit composing
			if(auto_commit > 0 && !mEnglishOnly){ //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				if(mComposing != null && mComposing.length() == auto_commit  && 
						activeSoftKeyboard != null && activeSoftKeyboard.indexOf("phone") != -1 ){
					InputConnection ic = getCurrentInputConnection();
					commitTyped(ic);
					//ic.commitText("", 0);
					//clearComposing();
				}
			}
		}
	}

	private void handleSeparator(int primaryCode) {
		if (mLIMEPref.getEnglishPrediction() && mPredictionOn
				&& ( !isPhysicalKeyPressed || mLIMEPref.getEnglishPredictionOnPhysicalKeyboard())){
			resetTempEnglishWord();
		}
		this.updateEnglishPrediction();
		sendKeyChar((char)primaryCode);
		
	}

	private AlertDialog mOptionsDialog;
	// Contextual menu positions
	private static final int POS_SETTINGS = 0;
	private static final int POS_HANCONVERT = 1;  //Jeremy '11,9,17
	private static final int POS_KEYBOARD = 2;
	private static final int POS_METHOD = 3;


	
	/**
	 * Add by Jeremy '10, 3, 24 for options menu in soft keyboard
	 */
	@TargetApi(11)
	private void handleOptions() {
		AlertDialog.Builder builder = null;
		
		if(android.os.Build.VERSION.SDK_INT < 11)
			builder = new AlertDialog.Builder(this);
		else
			builder = new AlertDialog.Builder(this, R.style.LIMEHDTheme);
			
		
		builder.setCancelable(true);
		builder.setIcon(R.drawable.sym_keyboard_done);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setTitle(getResources().getString(R.string.ime_name));

		CharSequence itemSettings = getString(R.string.lime_setting_preference);
		CharSequence hanConvert = getString(R.string.han_convert_option_list);

		CharSequence itemKeyboadList = getString(R.string.keyboard_list);
		CharSequence itemInputMethod = getString(R.string.input_method);
	
		builder.setItems(new CharSequence[] 
				{ itemSettings, hanConvert, itemKeyboadList, itemInputMethod}
				, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface di, int position) {
				di.dismiss();
				switch (position) {
				case POS_SETTINGS:
					launchSettings();
					break;
				case POS_HANCONVERT:  //Jeremy '11,9,17
					showHanConvertPicker();
					break;
				case POS_KEYBOARD:
					showIMPicker();
					break;
				case POS_METHOD:
					((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();				
					break;
			
					
				}
			}
		});

		mOptionsDialog = builder.create();
		Window window = mOptionsDialog.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.token = mInputView.getWindowToken();
		lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
		window.setAttributes(lp);
		window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		mOptionsDialog.show();
	}

	private void launchSettings() {
		handleClose();
		Intent intent = new Intent();
	    if(android.os.Build.VERSION.SDK_INT < 11)  //Jeremy '12,4,30 Add for deprecated preferenceActivity after API 11 (HC)
	    	intent.setClass(LIMEService.this, LIMEPreference.class);
	    else
	    	intent.setClass(LIMEService.this, LIMEPreferenceHC.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}


	private void nextActiveIM(boolean forward) { // forward: true, next IM; false prev. IM
		if(DEBUG) Log.i(TAG, "nextActiveIM()");
		buildActiveIMList();
		int i;
		CharSequence activeIMName = "";
		for (i = 0; i < activeIMCodeList.size(); i++) {
			if (activeIMCode.equals(activeIMCodeList.get(i))) {
				if (i == activeIMCodeList.size() - 1 && forward) {
					activeIMCode = activeIMCodeList.get(0);
					activeIMName = activeIMNameList.get(0);
				} else if(i == 0 && !forward){
					activeIMCode = activeIMCodeList.get(activeIMCodeList.size() - 1);
					activeIMName = activeIMNameList.get(activeIMCodeList.size() - 1);
				} else {
					activeIMCode = activeIMCodeList.get(i + ((forward)?1:-1));
					activeIMName = activeIMNameList.get(i + ((forward)?1:-1));
				}
				break;
			}
		}
		mLIMEPref.setActiveIM(activeIMCode);
		//Jeremy '12,4,21 force clear when switch to next keybaord
		clearComposing(true);
		// cancel candidate view if it's shown
		mEnglishOnly = false;
		mLIMEPref.setLanguageMode(false);
		//initialKeyboard();
		initialIMKeyboard();
		Toast.makeText(this, activeIMName, Toast.LENGTH_SHORT / 2).show();
		try {
			mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
			mKeyboardSwitcher.setImList(SearchSrv.getImList());
			//mKeyboardSwitcher.clearKeyboards();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		// Update keyboard xml information
		activeSoftKeyboard = mKeyboardSwitcher.getImKeyboard(activeIMCode);
	}

	private void buildActiveIMList() {
		CharSequence[] items = getResources().getStringArray(R.array.keyboard);
		CharSequence[] shortNames = getResources().getStringArray(R.array.keyboardShortname);
		CharSequence[] codes = getResources().getStringArray(
				R.array.keyboard_codes);
		
		String activeKeyboardState = mLIMEPref.getSelectedKeyboardState();
		
		if(!(mActiveKeyboardState.length()>0 && mActiveKeyboardState.equals(activeKeyboardState))) {

			mActiveKeyboardState = activeKeyboardState;

			String[] s = activeKeyboardState.toString().split(";");

			activeIMNameList.clear();
			activeIMCodeList.clear();
			activeIMShortNameList.clear();

			for (int i = 0; i < s.length; i++) {
				int index = Integer.parseInt(s[i]);

				if (index < items.length) {
					activeIMNameList.add(items[index].toString());
					activeIMShortNameList.add(shortNames[index].toString());
					activeIMCodeList.add(codes[index].toString());
					if(DEBUG) 
						Log.i(TAG, "buildActiveKeyboardList(): activekeyboard["+index+"] = "
								+ codes[index].toString() +" ;"+ shortNames[index].toString());
				} else {
					break;
				}
			}
		}
		if(DEBUG) Log.i(TAG, "curreneKeyboard:" + activeIMCode);
		// check if the selected keybaord is in active keybaord list.
		boolean matched = false;
		for (int i = 0; i < activeIMCodeList.size(); i++) {
			if (activeIMCode.equals(activeIMCodeList.get(i))) {
				if(DEBUG) Log.i(TAG, "buildActiveKeyboardList(): activekeyboard["+i+"] matches current keyboard: "+ activeIMCode);
				matched = true;
				break;
			}
		}
		if (!matched && SearchSrv!=null ) {
			// if the selected keyboard is not in the active keyboard list.
			// set the keyboard to the first active keyboard
			//if(DEBUG) Log.i(TAG, "currene keyboard is not in active list, reset to :" +  keyboardListCodes.get(0));
			
			activeIMCode = activeIMCodeList.get(0);
			//initializeIMKeyboard();
		}

	}
	/**
	 * Add by Jeremy '11,9,17 for han convert (tranditional <-> simplifed) options
	 */
	@TargetApi(11)
	private void showHanConvertPicker() {
		AlertDialog.Builder builder = null;
		
		if(android.os.Build.VERSION.SDK_INT < 11)
			builder = new AlertDialog.Builder(this);
		else
			builder = new AlertDialog.Builder(this, R.style.LIMEHDTheme);
			
		builder.setCancelable(true);
		builder.setIcon(R.drawable.sym_keyboard_done);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setTitle(getResources().getString(R.string.han_convert_option_list));
		CharSequence[] items =  getResources().getStringArray(R.array.han_convert_options);
		builder.setSingleChoiceItems(items,  mLIMEPref.getHanCovertOption(),
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface di, int position) {
						di.dismiss();
						handlHanConvertSelection(position);
					}
				});

		mOptionsDialog = builder.create();
		Window window = mOptionsDialog.getWindow();
		if (!(window == null)) {
			WindowManager.LayoutParams lp = window.getAttributes();
				lp.token = mCandidateView.getWindowToken(); 
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			window.setAttributes(lp);
			window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

		}
		mOptionsDialog.show();
	}
	private void handlHanConvertSelection(int position) {
		mLIMEPref.setHanCovertOption(position);
		
	}
	/**
	 * Add by Jeremy '10, 3, 24 for IM picker menu in options menu
	 * renamed to showIMPicker from showKeybaordPicer to avoid confusion '12,3,40
	 */
	@TargetApi(11)
	private void showIMPicker() {

		buildActiveIMList();

		AlertDialog.Builder builder = null;
		
		if(android.os.Build.VERSION.SDK_INT < 11)
			builder = new AlertDialog.Builder(this);
		else
			builder = new AlertDialog.Builder(this, R.style.LIMEHDTheme);
			
		builder.setCancelable(true);
		builder.setIcon(R.drawable.sym_keyboard_done);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setTitle(getResources().getString(R.string.keyboard_list));

		CharSequence[] items = new CharSequence[activeIMNameList.size()];// =
		// getResources().getStringArray(R.array.keyboard);
		int curKB = 0;
		for (int i = 0; i < activeIMNameList.size(); i++) {
			items[i] = activeIMNameList.get(i);
			if (activeIMCode.equals(activeIMCodeList.get(i)))
				curKB = i;
		}

		builder.setSingleChoiceItems(items, curKB,
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface di, int position) {
				di.dismiss();
				handleIMSelection(position);
			}
		});

		mOptionsDialog = builder.create();
		Window window = mOptionsDialog.getWindow();
		// Jeremy '10, 4, 12
		// The IM is not initialialized. do nothing here if window=null.
		if (!(window == null)) {
			WindowManager.LayoutParams lp = window.getAttributes();
			 // Jeremy '11,8,28 Use candidate instead of mInputview because mInputView may not present when using physical keyboard
			lp.token = mCandidateView.getWindowToken(); 
			lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
			window.setAttributes(lp);
			window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

		}
		mOptionsDialog.show();

	}

	private void handleIMSelection(int position) {
		if(DEBUG) Log.i(TAG, "handleIMSelection() position = " + position);

		activeIMCode = activeIMCodeList.get(position);
		
		mLIMEPref.setActiveIM(activeIMCode);
		//spe.putString("keyboard_list", keyboardSelection);
		//spe.commit();

		
		//Jeremy '12,4,21 foce clear when switch to selected keybaord
		clearComposing(true);

		//initialKeyboard();
		initialIMKeyboard();

		try {
			mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
			mKeyboardSwitcher.setImList(SearchSrv.getImList());
			//mKeyboardSwitcher.clearKeyboards();
			
			// Update soft keybaord information
			activeSoftKeyboard = mKeyboardSwitcher.getImKeyboard(activeIMCode);
			
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	public void onText(CharSequence text) {
		if (DEBUG)
			Log.i(TAG, "OnText()");
		InputConnection ic = getCurrentInputConnection();
		if (ic == null)
			return;
		ic.beginBatchEdit();

		if (mPredicting) {
			commitTyped(ic);
			//mJustRevertedSeparator = null;
		} else if (!mEnglishOnly &&mComposing.length() > 0) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
			pickDefaultCandidate();
			//	commitTyped(ic);
		}
		ic.commitText(text, 1);
		//ic.commitText(text, 0);
		
		ic.endBatchEdit();
		updateShiftKeyState(getCurrentInputEditorInfo());
	}
	
	private void updateCandidates() {
		this.updateCandidates(false);
	}
	
	
	private void updateChineseSymbol(){
		//ChineseSymbol chineseSym = new ChineseSymbol();
		isChineseSymbolSuggestionsShowing =true;
		List<Mapping> list = ChineseSymbol.getChineseSymoblList();
		if (list.size() > 0) {

			// Setup sel key display if 
			String selkey = "1234567890";
			if(disable_physical_selection && isPhysicalKeyPressed){
				selkey = "";
			}
			
			setSuggestions(list, isPhysicalKeyPressed, true, selkey);
			
			if(DEBUG) Log.i(TAG, "updateChineseSymbol():"
									+ "list.size:"+list.size());
		}
		
	}
	
	
	/**
	 * Update the list of available candidates from the current composing text.
	 * This will need to be filled in by however you are determining candidates.
	 */
	public void updateCandidates(boolean getAllRecords) {
		
		if(DEBUG) Log.i(TAG,"updateCandidate():Update Candidate mComposing:"+ mComposing);
		
		isChineseSymbolSuggestionsShowing = false;
		
		if (mComposing.length() > 0) {
			
			//showCandidateView();

			LinkedList<Mapping> list = new LinkedList<Mapping>();

			try {
				String keyString = mComposing.toString(), keynameString = "";
				
				//Art '30,Sep,2011 restrict the length of composing text for Stroke5
				if(activeSoftKeyboard.indexOf("wb") != -1){
					if(keyString.length() > 5){
						keyString = keyString.substring(0,5);
						mComposing = new StringBuilder();
						mComposing.append(keyString);
						InputConnection ic = getCurrentInputConnection();
										ic.setComposingText(keyString, 1);
					}
				}
				
				list.addAll(SearchSrv.query(keyString, !isPhysicalKeyPressed, getAllRecords));				
				//Jeremy '11,6,19 EZ and ETEN use "`" as IM Keys, and also custom may use "`".
				if (list.size() > 0) {
					String selkey=SearchSrv.getSelkey();
					String mixedModeSelkey = "`";
					if(hasSymbolMapping && !activeIMCode.equals("dayi") 
							&& ! (activeIMCode.equals("phonetic") 
									&& mLIMEPref.getPhoneticKeyboardType().equals("standard")) 	){
						mixedModeSelkey = " ";
					}
						
					
					int selkeyOption = mLIMEPref.getSelkeyOption();
					if(selkeyOption ==1) 	selkey = mixedModeSelkey +selkey;
					else if (selkeyOption ==2) 	selkey = mixedModeSelkey + " " +selkey;
					
					// Setup sel key display if 
					if(disable_physical_selection && isPhysicalKeyPressed){
						selkey = "";
					}
					
					setSuggestions(list, isPhysicalKeyPressed, true, selkey);
					
					if(DEBUG) Log.i(TAG, "updateCandidates(): display selkey:" + selkey 
											+ "list.size:"+list.size());
				} else {
					//Jermy '11,8,14
					clearSuggestions();
				}
				
				// Show composing window if keyToKeyname got different string. Revised by Jeremy '11,6,4
				if (SearchSrv.getTablename() != null ) {
						
					if (keyString != null && !keyString.equals("") ){//&& keyString.length() < 7) { Jeremy '11,8,30 move the limit to limedb					
						keynameString = SearchSrv.keyToKeyname(keyString); //.toLowerCase()); moved to LimeDB
							if (mCandidateView != null 
									&& !keynameString.toUpperCase().equals(keyString.toUpperCase())
									//&& !keynameString.equals("")
									&& !keynameString.trim().equals("")
									) {
								mCandidateView.setComposingText(keynameString);	
							}
					}
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		} else
			//Jermy '11,8,14
			clearSuggestions();
	}

	/*
	 * Update English dictionary view
	 */
	private void updateEnglishPrediction() {
		isChineseSymbolSuggestionsShowing = false;
		if (mPredictionOn && mLIMEPref.getEnglishPrediction()) {

			try {

				LinkedList<Mapping> list = new LinkedList<Mapping>();

				Mapping empty = new Mapping();
				empty.setWord("");
				empty.setDictionary(true);

				//Log.i("ART", "CACHE STRING -> " + tempEnglishWord.toString());
				if (tempEnglishWord == null || tempEnglishWord.length() == 0) {
					//list.add(empty);
					//Jermy '11,8,14
					clearSuggestions();
				} else {
					InputConnection ic = getCurrentInputConnection();
					if(ic==null) return;
					boolean after = false;
					try {
						char c = ic.getTextAfterCursor(1, 1).charAt(0);
						if (!Character.isLetterOrDigit(c)) {
							after = true;
						}
					} catch (StringIndexOutOfBoundsException e) {
						after = true;
					}

					boolean matchedtemp = false;

					if (tempEnglishWord.length() > 0) {
						try {
							if (tempEnglishWord.toString()
									.equalsIgnoreCase(
											ic.getTextBeforeCursor(
													tempEnglishWord.toString()
															.length(), 1)
													.toString())) {
								matchedtemp = true;
							}
						} catch (StringIndexOutOfBoundsException e) {
						}
					}

					if (after || matchedtemp) {

						tempEnglishList.clear();

						Mapping temp = new Mapping();
						temp.setWord(tempEnglishWord.toString());
						temp.setDictionary(true);

						List<Mapping> templist = SearchSrv
								.queryDictionary(tempEnglishWord.toString());

						if (templist.size() > 0) {
							list.add(temp);
							list.addAll(templist);
							
							// Setup sel key display if 
							String selkey = "1234567890";
							if(disable_physical_selection && isPhysicalKeyPressed){
								selkey = "";
							}
							
							setSuggestions(list, isPhysicalKeyPressed 
									, true, selkey);
							tempEnglishList.addAll(list);
						} else {
							//Jermy '11,8,14
							clearSuggestions();
						}
					}

				}

			} catch (Exception e) {
				Log.i("ART", "Error to update English predication");
			}
		}
	}

	/*
	 * Update dictionary view
	 */
	private void updateRelatedWord() {
		isChineseSymbolSuggestionsShowing = false;
		// Also use this to control whether need to display the english
		// suggestions words.

		// If there is no Temp Matched word exist then not to display dictionary
		try {
			// Modified by Jeremy '10, 4,1. getCode -> getWord
			// if( tempMatched != null && tempMatched.getCode() != null &&
			// !tempMatched.getCode().equals("")){
			if (tempMatched != null && tempMatched.getWord() != null
					&& !tempMatched.getWord().equals("")) {

				LinkedList<Mapping> list = new LinkedList<Mapping>();
				//Jeremy '11,8,9 Insert completion suggestions from application 
				//in front of related dictionary list in full-screen mode
				if(mCompletionOn ){
					list.addAll(buildCompletionList());
				}
				// Modified by Jeremy '10,3 ,12 for more specific related word
				// -----------------------------------------------------------
				if (tempMatched != null && hasMappingList) {
					list.addAll( SearchSrv.queryUserDic(tempMatched.getWord()));
				}
				// -----------------------------------------------------------
				if (list.size() > 0) {
					

					// Setup sel key display if 
					String selkey = "1234567890";
					if(disable_physical_selection && isPhysicalKeyPressed){
						selkey = "";
					}
					
					setSuggestions(list, isPhysicalKeyPressed  && !isFullscreenMode()
							, true, selkey);
				} else {
					tempMatched = null;
					//Jermy '11,8,14
					clearSuggestions();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private List<Mapping> buildCompletionList() {
		LinkedList<Mapping> list = new LinkedList<Mapping>();
		for (int i = 0; i < (mCompletions != null ? mCompletions.length : 0); i++) {
			CompletionInfo ci = mCompletions[i];
			if (ci != null) {
				Mapping temp = new Mapping();
				temp.setWord(ci.getText().toString());
				temp.setCode("");
				temp.setDictionary(true);
				list.add(temp);
			}
		}
		return list;
	}
	
	//Jeremy '11,8,21 update UI in handler 

	/*private void resetCandidateView(){
		if(DEBUG) Log.i(TAG,"showCandidateView()");
		mHandler.post(mShowCandidateView);
		try{
			super.setCandidatesViewShown(false);
		}catch(Exception e){
			e.printStackTrace();
		}
	}*/
	private void showCandidateView(){
		if(DEBUG) Log.i(TAG,"showCandidateView()");
		mHandler.post(mShowCandidateView);
	}
	private void hideCandidateView(){
		if(DEBUG) Log.i(TAG,"hideCandidateView()");
		isChineseSymbolSuggestionsShowing = false;
		mHandler.post(mHideCandidateView);
	}
	private void forceHideCandidateView(){
		if(DEBUG) Log.i(TAG,"forceHideCandidateView()");
		isChineseSymbolSuggestionsShowing = false;
		mHandler.post(mForceHideCandidateView);
	}
	
	final Handler mHandler = new Handler();
	// Create runnable for posting
	final Runnable mShowCandidateView = new Runnable() {
		public void run() {
			if(DEBUG)
				Log.i(TAG,"Runnable(): mShowCandidateView");
			
			setCandidatesViewShown(true);	
	    	}
	};
	final Runnable mHideCandidateView = new Runnable() {
		public void run() {
			//Jeremy '12,4,24 moved fixedcandidate here
			if(DEBUG)
				Log.i(TAG,"Runnable(): mHideCandidateView");
			if(mCandidateView == null 
					|| mLIMEPref.getFixedCandidateViewDisplay() ) return;  // escape if mCandidateView is not created '11,11,30 Jeremy
			if(isCandidateShown()){
				setCandidatesViewShown(false);	
			}
	    }
	};
	final Runnable mForceHideCandidateView = new Runnable() {
		public void run() {
			//Jeremy '12,4,24 ignore getfixedcanddiatedisplay
			if(DEBUG)
					Log.i(TAG,"Runnable(): mForceHideCandidateView");
			if(mCandidateView == null  ) return; 
			if(isCandidateShown()){
				setCandidatesViewShown(false);	
			}
	    }
	};
	
	public void setSuggestions(List<Mapping> suggestions, boolean showNumber,
			boolean typedWordValid, String diplaySelkey){
		if (suggestions != null && suggestions.size() > 0) {
			if(DEBUG) Log.i(TAG, "setSuggestion():suggestions.size="+ suggestions.size());
			showCandidateView();
			hasMappingList = true;

			if (mCandidateView != null) {
				templist = (LinkedList<Mapping>) suggestions;
				try {
					if (suggestions.size() == 1) {
						firstMatched = suggestions.get(0);
					} else if (suggestions.size() > 1) {
						firstMatched = suggestions.get(1);
					} else {
						firstMatched = null;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				mCandidateView.setSuggestions(suggestions, showNumber,
						typedWordValid, diplaySelkey);
			}
		} else {
			if(DEBUG) Log.i(TAG, "setSuggestion() with list=null");
			hasMappingList = false;
			//Jeremy '11,8,15
			clearSuggestions();
			
			
		}
		
	}
	
	

	private boolean isCandidateShown(){
		if(mCandidateView==null) {
			if(DEBUG)
				Log.i(TAG, "isCandidateViewShown(): mCandidateView is null");
			return false; //Jeremy '11,11,30 Fixed FC when startup, before mCandidate is created.
		}
		else return mCandidateView.isShown();
	}
	private void handleBackspace() {
		if(DEBUG) 
			Log.i(TAG, "handleBackspace()");
		final int length = mComposing.length();
		InputConnection ic=getCurrentInputConnection();
		if (length > 1) {
			mComposing.delete(length - 1, length);
			if(ic!=null) ic.setComposingText(mComposing, 1);
			updateCandidates();
		} else if (length == 1) {
			
			//Jeremy '12,4, 21 force clear the last characacter in composing
			clearComposing(true);
			//Jeremy '12,4,29 use mEnglishOnly instead of onIM
		} else if(!mEnglishOnly && mCandidateView !=null && isCandidateShown()  // composing length == 0 after here
				&& mLIMEPref.getAutoChineseSymbol()
				&& !isChineseSymbolSuggestionsShowing ){
			clearComposing(false);  //Jeremy '12,4,21 composing length 0, no need to force commit again. 
		} else if(!mEnglishOnly && mCandidateView !=null && isCandidateShown() &&
				!mLIMEPref.getFixedCandidateViewDisplay()){
			hideCandidateView();  //Jeremy '11,9,8
		} else {
			//Jeremy '11,8,15
			//clearSuggestions();
			try {
				if (mEnglishOnly && mLIMEPref.getEnglishPrediction()&& mPredictionOn
					&& ( !isPhysicalKeyPressed || mLIMEPref.getEnglishPredictionOnPhysicalKeyboard() )//mPredictionOnPhysicalKeyboard)
					) {
					if (tempEnglishWord != null && tempEnglishWord.length() > 0) {
						tempEnglishWord
								.deleteCharAt(tempEnglishWord.length() - 1);
						updateEnglishPrediction();
					}
					keyDownUp(KeyEvent.KEYCODE_DEL);
				} else{
				
					clearComposing(false); //Jeremy '12,4,21 composing length 0, no need to force commit again. 
					keyDownUp(KeyEvent.KEYCODE_DEL);
				}
			} catch (Exception e) {
				Log.i(TAG,"->" + e);
			}
		}

	}

	public void setCandidatesViewShown(boolean shown) {
		//if(mLIMEPref.getFixedCandidateViewDisplay()){//jeremy '12,4,24 moved to mhandler
		if(DEBUG)
			Log.i(TAG,"setCandidateViewShown():" + shown);
		if(shown)
			super.setCandidatesViewShown(shown);
		else
			super.setCandidatesViewShown(shown);
		
		if(DEBUG)
			Log.i(TAG, "isCandidateViewShown:" +isCandidateShown());
		
	}
	
	
	
	private void handleShift() {
		if(DEBUG) Log.i(TAG, "handleShift()");
		if (mInputView == null) {
			return;
		}

		if (mKeyboardSwitcher.isAlphabetMode()) {
			// Alphabet keyboard
			checkToggleCapsLock();
			mInputView.setShifted(mCapsLock || !mInputView.isShifted());
			mHasShift = mCapsLock || !mInputView.isShifted();
			if (mHasShift) {
				mKeyboardSwitcher.toggleShift();
			}
		} else {
			if (mCapsLock) {
				toggleCapsLock();
				mHasShift = false;
			} else if (mHasShift) {
				toggleCapsLock();
				mHasShift = true;
			} else {
				mKeyboardSwitcher.toggleShift();
				mHasShift = mKeyboardSwitcher.isShifted();

			}
		}
	}
	
	
	
	/**
	 * 
	 * Integrated all soft keyboards switching in this function.
	 */
	private void switchKeyboard(int primaryCode) {
		if(DEBUG) Log.i(TAG,"switchKeyboard() primaryCode = " +primaryCode);
		if (mCapsLock)
			toggleCapsLock();
		
		clearComposing(true);
		forceHideCandidateView();

		if (primaryCode == LIMEBaseKeyboard.KEYCODE_MODE_CHANGE) { //Symbol keyboard
			mEnglishOnly = true;
			mKeyboardSwitcher.toggleSymbols();
		} else if (primaryCode == KEYBOARD_SWITCH_CODE) { //Chi --> Eng
			mEnglishOnly = true;
			mLIMEPref.setLanguageMode(true);
			mKeyboardSwitcher.toggleChinese();
		} else if(primaryCode == KEYBOARD_SWITCH_IM_CODE){ //Eng --> Chi moved from SwitchKeyboardIM by Jeremy '12,4,29
			mEnglishOnly = false;
			mLIMEPref.setLanguageMode(false);
			initialIMKeyboard();
			
		}
			

		mHasShift = false;
		updateShiftKeyState(getCurrentInputEditorInfo());

		// Update keyboard xml information
		activeSoftKeyboard = mKeyboardSwitcher.getImKeyboard(activeIMCode);
	}

	
	/**
	 * For physical keybaord to switch between chinese and english mode. 
	 */
	private void switchChiEng() {  
		if(DEBUG)
			Log.i(TAG,"switchChiEng(): mEnglishOnly:" + mEnglishOnly);
		
		//Jeremy '12,4,21 force clear before switching chi/eng
		clearComposing(true);
		
		mKeyboardSwitcher.toggleChinese();
		mEnglishOnly = !mKeyboardSwitcher.isChinese();
		mLIMEPref.setLanguageMode(mEnglishOnly);
		
		if(DEBUG)
			Log.i(TAG, "switchChiEng(): mEnglishOnly updated as " + mEnglishOnly);
		
		
		if (mEnglishOnly) {
			Toast.makeText(this, R.string.typing_mode_english,
					Toast.LENGTH_SHORT / 2).show();
		} else {
			Toast.makeText(this, R.string.typing_mode_mixed,
					Toast.LENGTH_SHORT / 2).show();
		}
		clearSuggestions(); //Jeremy '11,9,5
	}

	

	private void initialViewAndSwitcher() {
		if(DEBUG)
			Log.i(TAG, "initialViewAndSwitcher()");

		// Check if mInputView == null;
		if (mInputView == null) {
			mInputView = (LIMEKeyboardView) getLayoutInflater().inflate(
					R.layout.input, null);
			mInputView.setOnKeyboardActionListener(this);
		}

		// Checkif mKeyboardSwitcher == null
		if (mKeyboardSwitcher == null) {
			mKeyboardSwitcher = new LIMEKeyboardSwitcher(this);
			mKeyboardSwitcher.setInputView(mInputView);
		}

		if (mKeyboardSwitcher.getKeyboardSize() == 0 && SearchSrv != null) {
			try {
				mKeyboardSwitcher.setKeyboardList(SearchSrv.getKeyboardList());
				mKeyboardSwitcher.setImList(SearchSrv.getImList());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}



	}

	/**
	 * For initializing Chinese IM and corresponding soft keyboards.
	 */
	private void initialIMKeyboard(){
		if(DEBUG)
			Log.i(TAG,"initalizeIMKeyboard(): keyboardSelection:" + activeIMCode);
		//mEnglishOnly = false;
		//super.setCandidatesViewShown(false);

		if (activeIMCode.equals("custom")) {
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
					LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
			
			hasNumberMapping = mLIMEPref.getAllowNumberMapping();
			hasSymbolMapping = mLIMEPref.getAllowSymoblMapping();
		}else if(activeIMCode.equals("cj")|| activeIMCode.equals("scj") || activeIMCode.equals("cj5") || activeIMCode.equals("ecj") ){
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
					LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
			hasNumberMapping = false;
			hasSymbolMapping = false;
		}else if (activeIMCode.equals("phonetic")){
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
					LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
			//Jeremy '11,6,18 ETEN 26 has no number mapping
			boolean standardPhonetic = !(mLIMEPref.getPhoneticKeyboardType().equals("eten26")
					||mLIMEPref.getPhoneticKeyboardType().equals("hsu"));
			hasNumberMapping = standardPhonetic; 
			hasSymbolMapping = standardPhonetic; 
		}else if(activeIMCode.equals("ez")|| activeIMCode.equals("dayi")) {
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
			LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
			hasNumberMapping = true;
			hasSymbolMapping = true;
		}else if (activeIMCode.equals("array10")) {
			hasNumberMapping = true;
			hasSymbolMapping = false;
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
				LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
		}else if (activeIMCode.equals("array")) {
			hasNumberMapping = true; //Jeremy '12,4,28 array 30 actually use number combination keys to enter symbols 
			hasSymbolMapping = true;
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
					LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
		}else if (activeIMCode.equals("wb")) {
			hasNumberMapping = false;
			hasSymbolMapping = true;
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
					LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
		}else if (activeIMCode.equals("hs")) {
			hasNumberMapping = true;
			hasSymbolMapping = true;
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
					LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
		}else {
			mKeyboardSwitcher.setKeyboardMode(activeIMCode,
					LIMEKeyboardSwitcher.MODE_TEXT, mImeOptions, true, false, false);
		}
		//Jeremy '11,9,3 for phone numeric key direct input on chacha
		if(mLIMEPref.getPhysicalKeyboardType().equals("chacha")) hasNumberMapping = false;
		String tablename = activeIMCode;
		if (tablename.equals("custom") || tablename.equals("phone")) {
			tablename = "custom";
		}
		//Jeremy '11,6,10 pass hasnumbermapping and hassymbolmapping to searchservice for selkey validation.
		if(DEBUG)
			Log.i(TAG, "switchKeyboard() current keyboard:" + 
					tablename+" hasnumbermapping:" +hasNumberMapping + " hasSymbolMapping:" + hasSymbolMapping);
		SearchSrv.setTablename(tablename, hasNumberMapping, hasSymbolMapping);
	}

	private boolean handleSelkey(int primaryCode, int[] keyCodes){
		
		// Jeremy '12,4,1 only do selkey on starndard keyboard
		
		// Check if disable physical key option is open
		if((disable_physical_selection && isPhysicalKeyPressed) || !mLIMEPref.getPhysicalKeyboardType().equals("normal_keyboard")){
			return false;
		}
		
		if(DEBUG) Log.i(TAG, "handleSelkey():primarycode:"+primaryCode);
		int i = -1;
		if (mComposing.length() > 0 && !mEnglishOnly) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
			// IM candidates view
			if(mLIMEPref.getSelkeyOption()>0 && primaryCode == 96) // "`"
				i=0 ;
			else{
				try {
					i = SearchSrv.isSelkey((char) primaryCode);
				} catch (RemoteException e) {

					e.printStackTrace();
				}
				if(i>=0) i = i + mLIMEPref.getSelkeyOption();
			}
			
		} else if(mEnglishOnly 
				|| (firstMatched != null && firstMatched.isDictionary()&& !mEnglishOnly)) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
			// related candidates view
			i = relatedSelkey.indexOf(primaryCode);
		} 
		
		
		if(i<0 || i >= templist.size()){
				return false;
		}
		else{
			pickSuggestionManually(i);
			return true;
		}
		
	}
	/**
	 * This method construct candidate view and add key code to composing object
	 * 
	 * @param primaryCode
	 * @param keyCodes
	 * @throws RemoteException 
	 */
	private void handleCharacter(int primaryCode, int[] keyCodes)  {
		//Jeremy '11,6,9 Cleaned code!!
		if(DEBUG)
			Log.i(TAG,"handleCharacter():primaryCode:" + primaryCode ); //+ "; keyCodes[0]:"+keyCodes[0]);

		// Adjust metakeystate on printed key pressed.
		if(isPhysicalKeyPressed)
			mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
		
		// Caculate key press time to handle Eazy IM keys mapping
		// 1,2,3,4,5,6 map to -(45) =(43) [(91) ](93) ,(44) \(92)
		String tablename="";
		tablename = SearchSrv.getTablename();
		if (keyPressTime != 0
				&& (System.currentTimeMillis() - keyPressTime > 700)
				&& tablename.equals("ez")){// mKeyboardSwitcher.getKeyboardMode() == LIMEKeyboardSwitcher.MODE_TEXT_EZ) {
			if (primaryCode == 49) {
				primaryCode = 45;
			} else if (primaryCode == 50) {
				primaryCode = 61;
			} else if (primaryCode == 51) {
				primaryCode = 91;
			} else if (primaryCode == 52) {
				primaryCode = 93;
			} else if (primaryCode == 53) {
				primaryCode = 39;
			} else if (primaryCode == 54) {
				primaryCode = 92;
			}
		}

		
		//Jeremy '11,6,6 processing physical keyboard selkeys.
		//Move here '11,6,9 to have lower priority than hasnumbermapping
		if (isPhysicalKeyPressed && (mCandidateView != null && isCandidateShown())) {
			if(handleSelkey(primaryCode, keyCodes))		{
				updateShiftKeyState(getCurrentInputEditorInfo());
				return;
			}
		}

			
		if (!mEnglishOnly) {
		
			if (DEBUG) 
				Log.i(TAG,"HandleCharacter():"
						+ "isValidLetter:"+ isValidLetter(primaryCode) 
						+ " isValidDigit:" + isValidDigit(primaryCode) 
						+ " isValideSymbol:" + isValidSymbol(primaryCode)
						+ " hasSymbolMapping:" + hasSymbolMapping
						+ " hasNumberMapping:" + hasNumberMapping
						+ " (primaryCode== MY_KEYCODE_SPACE && keyboardSelection.equals(phonetic):" + (primaryCode== MY_KEYCODE_SPACE && activeIMCode.equals("phonetic"))
						+ " mEnglishOnly:" + mEnglishOnly);
			
			
			if((!hasSymbolMapping)
					&& (primaryCode==','||primaryCode=='.') && !mEnglishOnly ){ // Chinese , and . processing //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				mComposing.append((char) primaryCode);
				InputConnection ic=getCurrentInputConnection();
				if(ic!=null) ic.setComposingText(mComposing, 1);
				updateCandidates();
				misMatched = mComposing.toString();
			}else if (!hasSymbolMapping && !hasNumberMapping  //Jeremy '11,10.19 fixed to bypass number key in et26 and hsu
					&&( isValidLetter(primaryCode) 
							|| (primaryCode== MY_KEYCODE_SPACE && activeIMCode.equals("phonetic")) ) //Jeremy '11,9,6 for et26 and hsu
					&& !mEnglishOnly) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				//Log.i(TAG,"handlecharacter(), onIM and no number and no symbol mapping");
				mComposing.append((char) primaryCode);
				InputConnection ic=getCurrentInputConnection();
				if(ic!=null) ic.setComposingText(mComposing, 1);
				updateCandidates();
				misMatched = mComposing.toString();
			} else if (!hasSymbolMapping
					&& hasNumberMapping
					&& (isValidLetter(primaryCode) || isValidDigit(primaryCode))
					&& !mEnglishOnly) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				mComposing.append((char) primaryCode);
				InputConnection ic=getCurrentInputConnection();
				if(ic!=null) ic.setComposingText(mComposing, 1);
				updateCandidates();
				misMatched = mComposing.toString();
			} else if (hasSymbolMapping
					&& !hasNumberMapping
					&& ( isValidLetter(primaryCode) || isValidSymbol(primaryCode)
							|| (primaryCode== MY_KEYCODE_SPACE && activeIMCode.equals("phonetic"))) //Jeremy '11,9,6 for chacha
					&& !mEnglishOnly) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				mComposing.append((char) primaryCode);
				InputConnection ic=getCurrentInputConnection();
				if(ic!=null) ic.setComposingText(mComposing, 1);
				updateCandidates();
				misMatched = mComposing.toString();
			} else if (hasSymbolMapping && !hasNumberMapping && activeIMCode.equals("array")
					&& mComposing != null && mComposing.length() >= 1
					&& getCurrentInputConnection().getTextBeforeCursor(1, 1).charAt(0) == 'w'
					&& Character.isDigit((char)primaryCode)
					&& !mEnglishOnly) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				// 27.May.2011 Art : This is the method to check user input type
				// if first previous character is w and second char is number then enable im mode.
				mComposing.append((char) primaryCode);
				InputConnection ic=getCurrentInputConnection();
				if(ic!=null) ic.setComposingText(mComposing, 1);
				updateCandidates();
				misMatched = mComposing.toString();
			} else if (hasSymbolMapping
					&& hasNumberMapping
					&& (isValidSymbol(primaryCode) 
							|| (primaryCode== MY_KEYCODE_SPACE && activeIMCode.equals("phonetic"))
							|| isValidLetter(primaryCode) || isValidDigit(primaryCode))	&& !mEnglishOnly) { //Jeremy '12,4,29 use mEnglishOnly instead of onIM
				mComposing.append((char) primaryCode);
				InputConnection ic=getCurrentInputConnection();
				if(ic!=null) ic.setComposingText(mComposing, 1);
				updateCandidates();
				misMatched = mComposing.toString();

			} else {

				if(!mEnglishOnly){ //Jeremy '12,4,29 use mEnglishOnly instead of onIM
					//Log.i(TAG,"handlecharacter(), onIM and default procedure");
					mCandidateView.takeSelectedSuggestion();  // check here.
					InputConnection ic=getCurrentInputConnection();
					if(ic!=null) ic.commitText(String.valueOf((char) primaryCode),1);
					//Jeremy '12,4,21
					finishComposing();
				} else{
					if (!mCandidateView.takeSelectedSuggestion()) {
						InputConnection ic=getCurrentInputConnection();
						if(ic!=null) ic.commitText(
								mComposing + String.valueOf((char) primaryCode),1);
					}else{
						InputConnection ic=getCurrentInputConnection();
						if(ic!=null) ic.commitText(String.valueOf((char) primaryCode),1);
						
					}
					
				}
				
			
			}
			
		} else {
			/*
			 * Handle when user input English Characters
			 */
			/*Log.i("ART",
					"English Only Software Keyboard :"
							+ String.valueOf((char) primaryCode));*/
			if (isInputViewShown()) {
				if (mInputView.isShifted()) {
					primaryCode = Character.toUpperCase(primaryCode);
				}
			}

			if (mLIMEPref.getEnglishPrediction() && mPredictionOn
					&& ( !isPhysicalKeyPressed || mLIMEPref.getEnglishPredictionOnPhysicalKeyboard())
				) {
				if (Character.isLetter((char) primaryCode)) {
					this.tempEnglishWord.append((char) primaryCode);
					this.updateEnglishPrediction();
				}
				else{ 
					resetTempEnglishWord();
					this.updateEnglishPrediction(); 
				  }
				
			}

				
			getCurrentInputConnection().commitText(
					String.valueOf((char) primaryCode), 1);
		}
		
		if(!(!isPhysicalKeyPressed && hasDistinctMultitouch))
			updateShiftKeyState(getCurrentInputEditorInfo());
	}

	private void handleClose() {
		if(DEBUG) Log.i(TAG,"handleClose()");
		// cancel candidate view if it's shown
		
		//Jeremy '12,4,23 need to check here.
		finishComposing();

		requestHideSelf(0);
		mInputView.closing();
	}

	private void checkToggleCapsLock() {

		if (mInputView.getKeyboard().isShifted()) {
			toggleCapsLock();
		}

	}

	private void toggleCapsLock() {
		mCapsLock = !mCapsLock;
		if (mKeyboardSwitcher.isAlphabetMode()) {
			((LIMEKeyboard) mInputView.getKeyboard()).setShiftLocked(mCapsLock);
		} else {
			if (mCapsLock) {
				if (DEBUG) {
					Log.i(TAG, "toggleCapsLock():mCapsLock:true");
				}
				if (!mKeyboardSwitcher.isShifted())
					mKeyboardSwitcher.toggleShift();
				((LIMEKeyboard) mInputView.getKeyboard()).setShiftLocked(true);
			} else {
				if (DEBUG) {
					Log.i(TAG,"toggleCapsLock():mCapsLock:false");
				}
				((LIMEKeyboard) mInputView.getKeyboard()).setShiftLocked(false);
				if (mKeyboardSwitcher.isShifted())
					mKeyboardSwitcher.toggleShift();
				// ((LIMEKeyboard) mInputView.getKeyboard()).setShifted(false);

			}
		}
	}

	public boolean isWordSeparator(int code) {
		//Jeremy '11,5,31
		String separators =  getResources().getString(R.string.word_separators);
        return separators.contains(String.valueOf((char)code));
		// String checkCode = String.valueOf((char)code);
		/*/ if (code == 32 || code == 39 || code == 10) {
		if (code == 32 || code == 10) {
			return true;
		} else {
			return false;
		}*/
	}

	public void pickDefaultCandidate() {
		//pickSuggestionManually(0);
		if(mCandidateView!=null)
			mCandidateView.takeSelectedSuggestion();
	}
	
	public void requestFullRecords() {
		if (DEBUG)
			Log.i(TAG,"requestFullRecords()");
	
		
		//updateCandidates to get full records.
		this.updateCandidates(true);
		
	}

	public void pickSuggestionManually(int index) {
		if (DEBUG)
			Log.i(TAG,"pickSuggestionManually():"
					+ "Pick up word at index : " + index + " templist.size()="+templist.size());

		// This is to prevent if user select the index more than the list
		if(templist != null && index >= templist.size() ){
			return;
		}
		/*//if "has_more_records" selected, updatecandidate with getAllRecords set.
		if(templist.get(index).getCode() != null 
				&& templist.get(index).getCode().equals("has_more_records")){
			this.updateCandidates(true);
			return;
		}*/
		
		
		if (templist != null && templist.size() > 0) {
			firstMatched = templist.get(index);
		}
		
		InputConnection ic=getCurrentInputConnection();
	
		if (mCompletionOn && mCompletions != null && index >= 0
				&& firstMatched.isDictionary()
				&& index < mCompletions.length ) {
			CompletionInfo ci = mCompletions[index];
			if(ic!=null) ic.commitCompletion(ci);
			if (DEBUG)
				Log.i(TAG, "pickSuggestionManually():mCompletionOn:" + mCompletionOn);

		} else if ((mComposing.length() > 0 
				||firstMatched != null && firstMatched.isDictionary()) && !mEnglishOnly) {  //Jeremy '12,4,29 use mEnglishOnly instead of onIM
			commitTyped(ic);
		} else if (mLIMEPref.getEnglishPrediction() && tempEnglishList != null
					&& tempEnglishList.size() > 0) {
				
				if(ic!=null) ic.commitText(
				this.tempEnglishList.get(index).getWord()
									.substring(tempEnglishWord.length())
									+ " ", 0);
					
				resetTempEnglishWord();

				Mapping temp = new Mapping();
				temp.setWord("");
				temp.setDictionary(true);
				//Jermy '11,8,14
				clearSuggestions();
			
		}
		
		if(activeSoftKeyboard.indexOf("wb") != -1){
			ic.setComposingText("", 0);
		}

	}

	void promoteToUserDictionary(String word, int frequency) {
		if (mUserDictionary.isValidWord(word))
			return;
		mUserDictionary.addWord(word, frequency);
	}

	public void swipeRight() {
		//if (mCompletionOn) {
		pickDefaultCandidate();
		//}
	}

	public void swipeLeft() {
		handleBackspace();
	}

	public void swipeDown() {
		handleClose();
	}

	public void swipeUp() {
		handleOptions();
	}

	/**
	 * First method to call after key press
	 */
	public void onPress(int primaryCode) {
		if(DEBUG) 
			Log.i(TAG, "onPress(): code = " + primaryCode);
		// Record key press time (press down)
		keyPressTime = System.currentTimeMillis();
		// To identify the source of character (Software keyboard or physical
		// keyboard)
		isPhysicalKeyPressed = false;
		
		if (hasDistinctMultitouch && primaryCode == LIMEBaseKeyboard.KEYCODE_SHIFT) {
			hasShiftPress = true;
			hasShiftCombineKeyPressed = false;
			handleShift();
		}else if (hasDistinctMultitouch && hasShiftPress){
			hasShiftCombineKeyPressed = true;
		}
		doVibrateSound(primaryCode);

		try {

			/*
			 * if
			 * (!mKeyboardSwitcher.getImKeyboard(keyboardSelection).equals("phone"
			 * )) { keyDownCode = primaryCode;
			 * 
			 * SharedPreferences sp1 = getSharedPreferences(PREF, 0); String
			 * xyvalue = sp1.getString("xy", ""); this.keyDownX =
			 * Float.parseFloat(xyvalue.split(",")[0]); this.keyDownY =
			 * Float.parseFloat(xyvalue.split(",")[1]); }
			 */

			//hasKeyPress = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doVibrateSound(int primaryCode) {
		if(DEBUG) Log.i(TAG,"doVibrateSound()");
		if (hasVibration) {
			//Jeremy '11,9,1 add preference on vibrate level
			mVibrator.vibrate(mLIMEPref.getVibrateLevel());
		}
		if (hasSound) {
			int sound = AudioManager.FX_KEYPRESS_STANDARD;
			switch (primaryCode) {
			case LIMEBaseKeyboard.KEYCODE_DELETE:
				sound = AudioManager.FX_KEYPRESS_DELETE;
				break;
			case MY_KEYCODE_ENTER:
				sound = AudioManager.FX_KEYPRESS_RETURN;
				break;
			case MY_KEYCODE_SPACE:
				sound = AudioManager.FX_KEYPRESS_SPACEBAR;
				break;
			}
			mAudioManager.playSoundEffect(sound, FX_VOLUME);
		}
	}

	/**
	 * Last method to execute when key release
	 */
	public void onRelease(int primaryCode) {
		if(DEBUG) 
			Log.i(TAG, "onRelease(): code = " + primaryCode);
		if(hasDistinctMultitouch && primaryCode == LIMEBaseKeyboard.KEYCODE_SHIFT ){
			hasShiftPress = false;
			if (hasShiftCombineKeyPressed) {
				hasShiftCombineKeyPressed = false;
				updateShiftKeyState(getCurrentInputEditorInfo());
			}
		}else if(hasDistinctMultitouch && !hasShiftPress){
			updateShiftKeyState(getCurrentInputEditorInfo());

		}
	}
	/*
	private final int UP = 0;
	private final int DOWN = 1;
	private final int LEFT = 2;
	private final int RIGHT = 3;

	public int handleSelection(float x, float y, String keys[]) {

		int result = 0;
		int direction;
		if (Math.abs(x) > Math.abs(y)) {
			// move horizontal
			if (x > 0) {
				direction = this.LEFT;
			} else {
				direction = this.RIGHT;
			}
		} else {
			// move verticle
			if (y > 0) {
				direction = this.UP;
			} else {
				direction = this.DOWN;
			}
		}

		// Select Character to be import
		result = (int) keys[direction].hashCode();

		return result;
	}
	*/
	public boolean isValidTime(Date target) {
		Calendar srcCal = Calendar.getInstance();
		srcCal.setTime(new Date());
		Calendar destCal = Calendar.getInstance();
		destCal.setTime(target);

		if (srcCal.getTimeInMillis() - destCal.getTimeInMillis() < 1800000) {
			return true;
		} else {
			return false;
		}

	}

	class AutoDictionary extends ExpandableDictionary {
		// If the user touches a typed word 2 times or more, it will become
		// valid.
		private static final int VALIDITY_THRESHOLD = 2 * FREQUENCY_FOR_PICKED;
		// If the user touches a typed word 5 times or more, it will be added to
		// the user dict.
		private static final int PROMOTION_THRESHOLD = 5 * FREQUENCY_FOR_PICKED;

		public AutoDictionary(Context context) {
			super(context);
		}

		@Override
		public boolean isValidWord(CharSequence word) {
			final int frequency = getWordFrequency(word);
			return frequency > VALIDITY_THRESHOLD;
		}

		@Override
		public void addWord(String word, int addFrequency) {
			final int length = word.length();
			// Don't add very short or very long words.
			if (length < 2 || length > getMaxWordLength())
				return;
			super.addWord(word, addFrequency);
			final int freq = getWordFrequency(word);
			if (freq > PROMOTION_THRESHOLD) {
				LIMEService.this.promoteToUserDictionary(word,
						FREQUENCY_FOR_AUTO_ADD);
			}
		}
	}
	
	

	@Override
	public void onDestroy() {
		if(DEBUG)
			Log.i(TAG,"onDestroy()");
		
		//jeremy 12,4,21 need to check again---
		//clearComposing(true); see no need to do this '12,4,21
		super.onDestroy();
/*
		if (SearchSrv != null) {
			try {
				this.unbindService(serConn);
			} catch (Exception e) {
				Log.i(TAG, "Failed to connect Search Service");
			}
		}
		*/
	}

	@Override
	public void onUpdateCursor(Rect newCursor) {
		if(DEBUG) 
			Log.i(TAG, "onUpdateCursor(): Top:" 
				+ newCursor.top + ". Right:" + newCursor.right
				+ ". bottom:" + newCursor.bottom + ". left:" + newCursor.left);
		
		
		if(mCandidateView!=null)
			mCandidateView.onUpdateCursor(newCursor);
		super.onUpdateCursor(newCursor);
	}

	@Override
	public void onCancel() {
		if(DEBUG)
			Log.i(TAG,"onCancel()");
		//clearComposing();  Jeremy '12,4,10 avoid clearcomposing when user slide outside the candidate area
		
	}
	//jeremy '11,9, 5 hideCanddiate when inputView is closed
	@Override
	public void updateInputViewShown() {
		if(DEBUG) Log.i(TAG, "updateInputViewShown(): mInputView.isShown(): " + mInputView.isShown());
		super.updateInputViewShown();
		if(!mInputView.isShown() && !isPhysicalKeyPressed)
			hideCandidateView();
	}
	
	/*
	 *Art '11,9, 26 auto commit composing
	 *
	public void autoCommit(){
		
	}*/


}