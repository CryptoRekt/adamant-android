package im.adamant.android.interactors;

import im.adamant.android.BuildConfig;
import im.adamant.android.Constants;
import im.adamant.android.core.AdamantApiWrapper;
import im.adamant.android.core.encryption.KeyStoreCipher;
import im.adamant.android.core.exceptions.PincodeInvalidException;
import im.adamant.android.helpers.PinCodeHelper;
import im.adamant.android.helpers.Settings;
import io.reactivex.Completable;
import io.reactivex.Flowable;

public class ValidatePinCodeInteractor {
    private KeyStoreCipher keyStoreCipher;
    private Settings settings;
    private PinCodeHelper pinCodeHelper;
    private AdamantApiWrapper api;

    private int attempts = 0;

    //This class is necessary for converting the pincode to json,
    // because the KeyStoreCipher does not know how to work with primitives
    public static class PincodeEntry {
        private String pincode;
        private int attempts;
        private long lastAttempt;

        public PincodeEntry(String pincode) {
            this.pincode = pincode;
        }

        public String getPincode() {
            return pincode;
        }

        public void setPincode(String pincode) {
            this.pincode = pincode;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public long getLastAttempt() {
            return lastAttempt;
        }

        public void setLastAttempt(long lastAttempt) {
            this.lastAttempt = lastAttempt;
        }
    }

    public ValidatePinCodeInteractor(
            KeyStoreCipher keyStoreCipher,
            Settings settings,
            PinCodeHelper pinCodeHelper,
            AdamantApiWrapper api
    ) {
        this.keyStoreCipher = keyStoreCipher;
        this.settings = settings;
        this.pinCodeHelper = pinCodeHelper;
        this.api = api;
    }

    public Completable createPincode(String pinCode) {
        return Completable.fromAction(() -> {
            PincodeEntry pincodeEntry = new PincodeEntry(pinCode);

            String encryptedPinCode = keyStoreCipher.encrypt(Constants.ADAMANT_PINCODE_ALIAS, pincodeEntry);
            settings.setEnablePincodeProtection(true);
            settings.setPincode(encryptedPinCode);

            if((settings.getAccountKeypair() != null) && (!settings.getAccountKeypair().isEmpty())) {

                PinCodeHelper.SignedKeyPair signedKeyPair = pinCodeHelper.buildSignedKeyPair(api.getKeyPair());
                String account = keyStoreCipher.encrypt(Constants.ADAMANT_ACCOUNT_ALIAS, signedKeyPair);
                settings.setAccountKeypair(account);
            }
        });
    }

    public Completable verifyPincode(String pinCode) {
        return Completable.fromAction(() -> {
            String encryptedPincode = settings.getPincode();
            PincodeEntry decryptedPincodeEntry = null;
            try {
                decryptedPincodeEntry = keyStoreCipher.decrypt(
                        Constants.ADAMANT_PINCODE_ALIAS,
                        encryptedPincode,
                        PincodeEntry.class
                );
            } catch (Exception ex){
                settings.setPincode("");
                settings.setEnablePincodeProtection(false);
                throw new PincodeInvalidException(attempts, PincodeInvalidException.Reason.INVALID);
            }

            if (
                decryptedPincodeEntry == null ||
                decryptedPincodeEntry.getPincode() == null ||
                decryptedPincodeEntry.getPincode().isEmpty()
            ) {
                attempts++;
                throw new PincodeInvalidException(attempts, PincodeInvalidException.Reason.INVALID);
            }

            try {
                attempts = decryptedPincodeEntry.getAttempts() + 1;

                if (attempts > BuildConfig.MAX_WRONG_PINCODE_ATTEMPTS){
                    long threshold = decryptedPincodeEntry.getLastAttempt() + BuildConfig.DELAY_BETWEEN_PINCODE_ATTEMPTS;
                    if (threshold < System.currentTimeMillis()){
                        attempts = 0;
                    } else {
                        throw new PincodeInvalidException(attempts, PincodeInvalidException.Reason.TIMEOUT);
                    }
                }

                if (pinCode == null || pinCode.isEmpty()){
                    throw new PincodeInvalidException(attempts, PincodeInvalidException.Reason.EMPTY);
                }

                boolean isMatch = pinCode.equalsIgnoreCase(decryptedPincodeEntry.getPincode());

                if (isMatch) {
                    attempts = 0;
                } else {
                    throw new PincodeInvalidException(attempts, PincodeInvalidException.Reason.NOT_MATCH);
                }

            } finally {
                decryptedPincodeEntry.setAttempts(attempts);
                decryptedPincodeEntry.setLastAttempt(System.currentTimeMillis());

                String encryptedPincodeEntry = keyStoreCipher.encrypt(
                        Constants.ADAMANT_PINCODE_ALIAS,
                        decryptedPincodeEntry
                );

                settings.setPincode(encryptedPincodeEntry);
            }
        });
    }
}
