package im.adamant.android.presenters;

import android.webkit.URLUtil;

import com.arellomobile.mvp.InjectViewState;

import java.util.Set;

import im.adamant.android.Screens;
import im.adamant.android.core.entities.ServerNode;
import im.adamant.android.helpers.Settings;
import im.adamant.android.interactors.SaveKeypairInteractor;
import im.adamant.android.interactors.ServerNodeInteractor;
import im.adamant.android.interactors.SubscribeToPushInteractor;
import im.adamant.android.interactors.ValidatePinCodeInteractor;
import im.adamant.android.ui.mvp_view.SettingsView;
import io.reactivex.disposables.CompositeDisposable;
import ru.terrakok.cicerone.Router;

@InjectViewState
public class SettingsPresenter extends  BasePresenter<SettingsView> {
    private Settings settings;
    private ServerNodeInteractor serverNodeInteractor;
    private Router router;

    public SettingsPresenter(
            Settings settings,
            ServerNodeInteractor serverNodeInteractor,
            Router router,
            CompositeDisposable subscriptions
    ) {
        super(subscriptions);
        this.settings = settings;
        this.serverNodeInteractor = serverNodeInteractor;
        this.router = router;
    }

    @Override
    public void attachView(SettingsView view) {
        super.attachView(view);
        getViewState().setStoreKeyPairOption(
                settings.isKeyPairMustBeStored()
        );
        getViewState().setEnablePushOption(
                settings.isEnablePushNotifications()
        );
        getViewState().setAddressPushService(
                settings.getAddressOfNotificationService()
        );
    }

    public void onClickAddNewNode(String nodeUrl) {
        if (URLUtil.isValidUrl(nodeUrl)){
            serverNodeInteractor.addServerNode(nodeUrl);
            getViewState().clearNodeTextField();
            getViewState().hideKeyboard();
        }
    }

    public void onClickSaveSettings(){
        getViewState().callSaveSettingsService();
    }

    public void onClickDeleteNode(ServerNode serverNode){
        serverNodeInteractor.deleteNode(serverNode);
    }

    public void onClickEnablePincode(boolean enabled) {
        if (enabled){
            router.navigateTo(Screens.PINCODE_SCREEN);
        } else {
            settings.setPincode("");
            settings.setEnablePincodeProtection(false);
        }
    }
}
