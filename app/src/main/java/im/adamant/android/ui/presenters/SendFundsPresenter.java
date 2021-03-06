package im.adamant.android.ui.presenters;

import com.arellomobile.mvp.InjectViewState;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import im.adamant.android.BuildConfig;
import im.adamant.android.Screens;
import im.adamant.android.helpers.ChatsStorage;
import im.adamant.android.helpers.LoggerHelper;
import im.adamant.android.helpers.PublicKeyStorage;
import im.adamant.android.interactors.SendFundsInteractor;
import im.adamant.android.interactors.wallets.SupportedWalletFacadeType;
import im.adamant.android.interactors.wallets.WalletFacade;
import im.adamant.android.ui.entities.Chat;
import im.adamant.android.ui.messages_support.factories.MessageFactoryProvider;
import im.adamant.android.ui.mvp_view.SendFundsView;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import ru.terrakok.cicerone.Router;

@InjectViewState
public class SendFundsPresenter extends BasePresenter<SendFundsView> {
    private Router router;
    private Map<SupportedWalletFacadeType, WalletFacade> wallets;
    private SendFundsInteractor sendCurrencyInteractor;
    private PublicKeyStorage publicKeyStorage;
    private ChatsStorage chatsStorage;
    private MessageFactoryProvider messageFactoryProvider;

    private String companionId;
    private SupportedWalletFacadeType facadeType;
    private WalletFacade currentFacade;

    private BigDecimal currentAmount = BigDecimal.ZERO;
    private String comment = "";

    public SendFundsPresenter(
            Router router,
            Map<SupportedWalletFacadeType, WalletFacade> wallets,
            SendFundsInteractor sendCurrencyInteractor,
            MessageFactoryProvider messageFactoryProvider,
            PublicKeyStorage publicKeyStorage,
            ChatsStorage chatsStorage,
            CompositeDisposable subscriptions
    ) {
        super(subscriptions);
        this.router = router;
        this.wallets = wallets;
        this.sendCurrencyInteractor = sendCurrencyInteractor;
        this.publicKeyStorage = publicKeyStorage;
        this.messageFactoryProvider = messageFactoryProvider;
        this.chatsStorage = chatsStorage;
    }

    public void setCompanionIdAndFacadeType(String companionId, SupportedWalletFacadeType type) {
        this.companionId = companionId;
        this.facadeType = type;

        currentFacade = wallets.get(type);

        getViewState().setFundsSendingIsSupported(
                currentFacade.isSupportFundsSending()
        );

        getViewState().setRecipientAddress(
                currentFacade.getCurrencyAddress(
                        companionId,
                        publicKeyStorage.getPublicKey(companionId)
                )
        );

        if (currentFacade.isSupportComment()){
            getViewState().showCommentField();
        } else {
            getViewState().hideCommentField();
        }

        int resourceId = currentFacade.getIconForEditText();
        if (resourceId != 0) {
            getViewState().setEditTextIcons(resourceId);
        }

        Chat chat = chatsStorage.findChatByCompanionId(companionId);
        if (chat != null) {
            getViewState().setRecipientName(chat.getTitle());
        }

        Disposable balanceUpdateFlowable = Flowable
                .interval(0, BuildConfig.UPDATE_BALANCE_SECONDS_DELAY, TimeUnit.SECONDS)
                .withLatestFrom(currentFacade.getFee(), (i, fee) -> fee)
                .subscribe(
                        fee -> {
                            BigDecimal balance = currentFacade.getBalance();
                            calculate(currentAmount, balance, fee);
                        },
                        error -> LoggerHelper.e("UPDATED BALANCE", error.getMessage(), error)
                );
        subscriptions.add(balanceUpdateFlowable);

    }

    public void onEnterAmount(BigDecimal amount) {
        if (amount != null){
            currentAmount = amount;
            Disposable subscribe = currentFacade
                    .getFee()
                    .subscribe(
                        fee -> calculate(amount, currentFacade.getBalance(), fee),
                        error -> LoggerHelper.e("amount", error.getMessage(), error)
                    );
            subscriptions.add(subscribe);
        }
    }

    public void onClickSendButton() {
        getViewState().showTransferConfirmationDialog(currentAmount, facadeType.name(), companionId);
    }

    public void onClickConfirmSend() {
        Disposable subscribe = sendCurrencyInteractor
                .sendCurrency(companionId, comment, currentAmount, facadeType)
                .subscribe(
                        transactionWasProcessed -> {
                            router.backTo(Screens.MESSAGES_SCREEN);
                        },
                        error -> {
                            router.showSystemMessage(error.getMessage());
                        }
                );

        subscriptions.add(subscribe);
    }

    private void calculate(BigDecimal amount, BigDecimal balance, BigDecimal fee) {

        getViewState().setFee(fee, facadeType.name());

        BigDecimal totalAmount = amount.add(fee);
        getViewState().setTotalAmount(totalAmount, facadeType.name());

        getViewState().setCurrentBalance(balance, facadeType.name());

        BigDecimal reminder = balance.subtract(totalAmount);
        getViewState().setReminder(reminder, facadeType.name());

        boolean isSendButtonMustLocked = (reminder.compareTo(BigDecimal.ZERO) < 0) || (amount.compareTo(BigDecimal.ZERO) <= 0);

        if (isSendButtonMustLocked) {
            getViewState().lockSendButton();
        } else {
            getViewState().unlockSendButton();
        }
    }
}
