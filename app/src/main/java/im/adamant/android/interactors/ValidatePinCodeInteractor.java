package im.adamant.android.interactors;

import im.adamant.android.Constants;
import im.adamant.android.core.encryption.KeyStoreCipher;
import im.adamant.android.helpers.Settings;
import io.reactivex.Completable;
import io.reactivex.Flowable;

public class ValidatePinCodeInteractor {
    private KeyStoreCipher keyStoreCipher;
    private Settings settings;

    //This class is necessary for converting the pincode to json,
    // because the KeyStoreCipher does not know how to work with primitives
    static class PincodeEntry {
        private String pincode;

        public PincodeEntry(String pincode) {
            this.pincode = pincode;
        }

        public String getPincode() {
            return pincode;
        }

        public void setPincode(String pincode) {
            this.pincode = pincode;
        }
    }

    public ValidatePinCodeInteractor(KeyStoreCipher keyStoreCipher, Settings settings) {
        this.keyStoreCipher = keyStoreCipher;
        this.settings = settings;
    }

    public Completable createPincode(String pinCode) {
        return Completable.fromAction(() -> {
            PincodeEntry pincodeEntry = new PincodeEntry(pinCode);

            String encryptedPinCode = keyStoreCipher.encrypt(Constants.ADAMANT_PINCODE_ALIAS, pincodeEntry);
            settings.setEnablePincodeProtection(true);
            settings.setPincode(encryptedPinCode);
        });
    }

    public Flowable<Boolean> verifyPincode(String pinCode) {
        return Flowable.fromCallable(() -> {
            if (pinCode == null || pinCode.isEmpty()){return false;}

            String encryptedPincode = settings.getPincode();
            PincodeEntry decryptedPincodeEntry = keyStoreCipher.decrypt(Constants.ADAMANT_PINCODE_ALIAS, encryptedPincode, PincodeEntry.class);

            if (
                decryptedPincodeEntry == null ||
                decryptedPincodeEntry.getPincode() == null ||
                decryptedPincodeEntry.getPincode().isEmpty()
            ) {
                return false;
            }

            return pinCode.equalsIgnoreCase(decryptedPincodeEntry.getPincode());
        });
    }
}
