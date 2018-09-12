package im.adamant.android.interactors;

import com.goterl.lazycode.lazysodium.utils.KeyPair;

import java.util.concurrent.Callable;

import im.adamant.android.Constants;
import im.adamant.android.core.AdamantApiWrapper;
import im.adamant.android.core.encryption.AdamantKeyGenerator;
import im.adamant.android.core.encryption.KeyStoreCipher;
import im.adamant.android.core.exceptions.IncompatibleSignatureException;
import im.adamant.android.core.responses.Authorization;

import im.adamant.android.helpers.LoggerHelper;
import im.adamant.android.helpers.PinCodeHelper;
import im.adamant.android.helpers.Settings;
import im.adamant.android.rx.ObservableRxList;
import io.github.novacrypto.bip39.MnemonicValidator;
import io.github.novacrypto.bip39.Validation.InvalidChecksumException;
import io.github.novacrypto.bip39.Validation.InvalidWordCountException;
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException;
import io.github.novacrypto.bip39.Validation.WordNotFoundException;
import io.github.novacrypto.bip39.wordlists.English;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class AuthorizeInteractor {

    private AdamantApiWrapper api;
    private AdamantKeyGenerator keyGenerator;
    private KeyStoreCipher keyStoreCipher;
    private Settings settings;
    private PinCodeHelper pinCodeHelper;


    public AuthorizeInteractor(
            AdamantApiWrapper api,
            AdamantKeyGenerator keyGenerator,
            KeyStoreCipher keyStoreCipher,
            PinCodeHelper pinCodeHelper,
            Settings settings
    ) {
        this.api = api;
        this.keyGenerator = keyGenerator;
        this.keyStoreCipher = keyStoreCipher;
        this.pinCodeHelper = pinCodeHelper;
        this.settings = settings;
    }

    public Flowable<Authorization> authorize(String passPhrase) {
        try {
            return api.authorize(passPhrase)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(authorization -> {
                        if (settings.isKeyPairMustBeStored()){
                            KeyPair keyPair = api.getKeyPair();

                            PinCodeHelper.SignedKeyPair signedKeyPair = null;
                            if (settings.isEnablePincodeProtection()){
                                signedKeyPair = pinCodeHelper.buildSignedKeyPair(keyPair);
                            } else {
                                signedKeyPair = pinCodeHelper.buildUnsignedKeyPair(keyPair);
                            }

                            String account = keyStoreCipher.encrypt(Constants.ADAMANT_ACCOUNT_ALIAS, signedKeyPair);
                            settings.setAccountKeypair(account);
                        }
                    })
                    .onErrorResumeNext(error -> {
                        LoggerHelper.e("authorization", error.getMessage(), error);
                        return Flowable.just(new Authorization());
                    });
        }catch (Exception ex){
            ex.printStackTrace();
            return Flowable.error(ex);
        }
    }

    public Flowable<Authorization> restoreAuthorization() {

            //Not transform this code in lambda (Application crashed if unchecked exception)
           return Flowable.fromCallable(new Callable<KeyPair>() {
                    @Override
                    public KeyPair call() throws Exception {
                        String account = settings.getAccountKeypair();
                        if (account == null || account.isEmpty()){
                            throw new Exception("Account not stored!");
                        }

                        PinCodeHelper.SignedKeyPair signedKeyPair = keyStoreCipher.decrypt(
                                Constants.ADAMANT_ACCOUNT_ALIAS,
                                account,
                                PinCodeHelper.SignedKeyPair.class
                        );

                        if (signedKeyPair == null) {
                            throw new Exception("Account not decrypted!");
                        }

                        return pinCodeHelper.restoreKeyPairFromSignedKeypair(signedKeyPair);
                    }
                })
               .subscribeOn(Schedulers.computation())
               .flatMap(keyPair -> api.authorize(keyPair));

    }

    public CharSequence generatePassPhrase() {
        return keyGenerator.generateNewPassphrase();
    }

    public boolean isValidPassphrase(String passphrase) {
        try {
            MnemonicValidator
                    .ofWordList(English.INSTANCE)
                    .validate(passphrase);
        } catch (InvalidChecksumException | InvalidWordCountException | WordNotFoundException | UnexpectedWhiteSpaceException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public Flowable<Authorization> createNewAccount(String passPhrase) {
        return api.createNewAccount(passPhrase)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public boolean isAuthorized() {
        return api.isAuthorized();
    }
}
