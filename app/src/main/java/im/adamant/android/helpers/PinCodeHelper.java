package im.adamant.android.helpers;

import com.goterl.lazycode.lazysodium.utils.KeyPair;

import im.adamant.android.Constants;
import im.adamant.android.core.encryption.KeyStoreCipher;
import im.adamant.android.core.exceptions.IncompatibleSignatureException;
import im.adamant.android.interactors.ValidatePinCodeInteractor;

public class PinCodeHelper {
    private Settings settings;
    private KeyStoreCipher keyStoreCipher;

    public static class SignedKeyPair {
        private String sign;
        private KeyPair keyPair;

        public String getSign() {
            return sign;
        }

        public void setSign(String sign) {
            this.sign = sign;
        }

        public KeyPair getKeyPair() {
            return keyPair;
        }

        public void setKeyPair(KeyPair keyPair) {
            this.keyPair = keyPair;
        }
    }

    public PinCodeHelper(Settings settings, KeyStoreCipher keyStoreCipher) {
        this.settings = settings;
        this.keyStoreCipher = keyStoreCipher;
    }

    public SignedKeyPair buildSignedKeyPair(KeyPair keyPair) throws Exception {
        String sign = "";

        String encryptedPincode = settings.getPincode();
        ValidatePinCodeInteractor.PincodeEntry decryptedPincodeEntry = keyStoreCipher.decrypt(Constants.ADAMANT_PINCODE_ALIAS, encryptedPincode, ValidatePinCodeInteractor.PincodeEntry.class);

        if (decryptedPincodeEntry == null) {throw new Exception("Empty pincode");}

        sign = keyStoreCipher.signDataByPincode(Constants.ADAMANT_ACCOUNT_ALIAS, keyPair, decryptedPincodeEntry.getPincode());

        SignedKeyPair signedKeyPair = new SignedKeyPair();
        signedKeyPair.setKeyPair(keyPair);
        signedKeyPair.setSign(sign);

        return signedKeyPair;
    }

    public SignedKeyPair buildUnsignedKeyPair(KeyPair keyPair) {
        SignedKeyPair signedKeyPair = new SignedKeyPair();
        signedKeyPair.setKeyPair(keyPair);
        signedKeyPair.setSign("");

        return signedKeyPair;
    }

    public KeyPair restoreKeyPairFromSignedKeypair(SignedKeyPair signedKeyPair) throws Exception {
        String sign = signedKeyPair.getSign();

        if(sign == null || sign.isEmpty()) {
            return signedKeyPair.getKeyPair();
        }

        //Check sign
        String encryptedPincode = settings.getPincode();
        ValidatePinCodeInteractor.PincodeEntry decryptedPincodeEntry = keyStoreCipher.decrypt(
                Constants.ADAMANT_PINCODE_ALIAS,
                encryptedPincode,
                ValidatePinCodeInteractor.PincodeEntry.class
        );

        try {
            if (decryptedPincodeEntry == null || decryptedPincodeEntry.getPincode().isEmpty()){
                throw new IncompatibleSignatureException();
            }

            boolean validated = keyStoreCipher.validateSign(
                    Constants.ADAMANT_ACCOUNT_ALIAS,
                    signedKeyPair.getKeyPair(),
                    decryptedPincodeEntry.getPincode(),
                    sign
            );

            if (validated){
                return signedKeyPair.getKeyPair();
            } else {
                throw new IncompatibleSignatureException();
            }
        } catch (IncompatibleSignatureException ex){
            settings.setAccountKeypair("");
            settings.setPincode("");
            settings.setEnablePincodeProtection(false);

            throw ex;
        }

    }
}
