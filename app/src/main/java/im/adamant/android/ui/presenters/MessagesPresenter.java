package im.adamant.android.ui.presenters;


import com.arellomobile.mvp.InjectViewState;

import im.adamant.android.Screens;
import im.adamant.android.core.AdamantApi;
import im.adamant.android.core.AdamantApiWrapper;
import im.adamant.android.core.exceptions.NotAuthorizedException;
import im.adamant.android.helpers.BalanceConvertHelper;
import im.adamant.android.helpers.LoggerHelper;
import im.adamant.android.interactors.ChatUpdatePublicKeyInteractor;
import im.adamant.android.interactors.RefreshChatsInteractor;
import im.adamant.android.helpers.ChatsStorage;
import im.adamant.android.ui.entities.Chat;
import im.adamant.android.ui.messages_support.SupportedMessageListContentType;
import im.adamant.android.ui.messages_support.entities.AdamantBasicMessage;
import im.adamant.android.ui.messages_support.entities.MessageListContent;
import im.adamant.android.ui.messages_support.factories.AdamantBasicMessageFactory;
import im.adamant.android.ui.messages_support.factories.MessageFactoryProvider;
import im.adamant.android.ui.messages_support.processors.MessageProcessor;
import im.adamant.android.ui.mvp_view.MessagesView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import ru.terrakok.cicerone.Router;

@InjectViewState
public class MessagesPresenter extends BasePresenter<MessagesView>{
    private Router router;
    private RefreshChatsInteractor refreshChatsInteractor;
    private ChatsStorage chatsStorage;
    private MessageFactoryProvider messageFactoryProvider;
    private ChatUpdatePublicKeyInteractor chatUpdatePublicKeyInteraactor;
    private AdamantApiWrapper api;

    private Chat currentChat;
    private List<MessageListContent> messages;
    private int currentMessageCount = 0;

    private Disposable syncSubscription;

    public MessagesPresenter(
            Router router,
            RefreshChatsInteractor refreshChatsInteractor,
            ChatUpdatePublicKeyInteractor chatUpdatePublicKeyInteraactor,
            MessageFactoryProvider messageFactoryProvider,
            ChatsStorage chatsStorage,
            AdamantApiWrapper api,
            CompositeDisposable subscriptions
    ) {
        super(subscriptions);
        this.router = router;
        this.refreshChatsInteractor = refreshChatsInteractor;
        this.chatUpdatePublicKeyInteraactor = chatUpdatePublicKeyInteraactor;
        this.messageFactoryProvider = messageFactoryProvider;
        this.chatsStorage = chatsStorage;
        this.api = api;
    }


    @Override
    public void attachView(MessagesView view) {
        super.attachView(view);

        syncSubscription = refreshChatsInteractor
                .execute()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError((error) -> {
                    if (error instanceof NotAuthorizedException){
                        router.navigateTo(Screens.SPLASH_SCREEN);
                    } else {
                        router.showSystemMessage(error.getMessage());
                    }
                    LoggerHelper.e("Messages", error.getMessage(), error);
                })
                .doOnComplete(() -> {
                    if (currentMessageCount != messages.size()){
                        getViewState().showChatMessages(messages);
                        currentMessageCount = messages.size();
                    }
                })
                .retryWhen((retryHandler) -> retryHandler.delay(AdamantApi.SYNCHRONIZE_DELAY_SECONDS, TimeUnit.SECONDS))
                .repeatWhen((completed) -> completed.delay(AdamantApi.SYNCHRONIZE_DELAY_SECONDS, TimeUnit.SECONDS))
                .subscribe();

        subscriptions.add(syncSubscription);
    }

    @Override
    public void detachView(MessagesView view) {
        super.detachView(view);
        if (syncSubscription != null){
            syncSubscription.dispose();
        }
    }

    public void onShowChatByCompanionId(String companionId){
        currentChat = chatsStorage.findChatByCompanionId(companionId);
        if (currentChat == null){return;}

        messages = chatsStorage.getMessagesByCompanionId(
                companionId
        );

        getViewState().changeTitles(currentChat.getTitle(), currentChat.getCompanionId());

        getViewState()
            .showChatMessages(
                messages
            );
    }

    public void onResume() {
        if (currentChat != null) {
            getViewState().changeTitles(currentChat.getTitle(), currentChat.getCompanionId());
        }
    }

    public void onClickCopyAddress() {
        if (currentChat != null) {
            getViewState().copyCompanionId(currentChat.getCompanionId());
        }
    }

    public void onClickShowQrCodeAddress() {
        if (currentChat != null) {
            getViewState().showQrCodeCompanionId(currentChat.getCompanionId());
        }
    }

    public void onShowChatByAddress(String address, String label){
        Chat chat = new Chat();
        chat.setCompanionId(address);
        if (label != null && !label.isEmpty()){
            chat.setTitle(label);
        } else {
            chat.setTitle(address);
        }

        Disposable subscribe = chatUpdatePublicKeyInteraactor
                .execute(chat)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        chatWithKey -> {
                            chatsStorage.addNewChat(chatWithKey);
                            onShowChatByCompanionId(address);
                        },
                        error -> LoggerHelper.e("messagePresenter", error.getMessage())
                );
        subscriptions.add(subscribe);
    }

    public void onClickSendAdamantBasicMessage(String message){
        if (message.trim().isEmpty()) {
            //TODO: notify user about empty message
            return;
        }

        if (currentChat == null){return;}

        try {
            AdamantBasicMessageFactory messageFactory = (AdamantBasicMessageFactory) messageFactoryProvider.getFactoryByType(SupportedMessageListContentType.ADAMANT_BASIC);
            AdamantBasicMessage messageEntity = getAdamantMessage(message, messageFactory);
            chatsStorage.addMessageToChat(messageEntity);

            MessageProcessor<AdamantBasicMessage> messageProcessor = messageFactory.getMessageProcessor();

            Disposable subscription = messageProcessor
                    .sendMessage(messageEntity)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((transaction -> {
                                if (transaction.isSuccess()){
                                    messageEntity.setProcessed(true);
                                    messageEntity.setTransactionId(transaction.getTransactionId());
                                }

                                getViewState().messageWasSended(messageEntity);
                            }),
                            (error) -> {
                                router.showSystemMessage(error.getMessage());
                                error.printStackTrace();
                            }
                    );

            subscriptions.add(subscription);

            getViewState().goToLastMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onChangeMessageText(String text) {
        //TODO: You need to navigate by the type of message that is being edited
        try {
            AdamantBasicMessageFactory messageFactory = (AdamantBasicMessageFactory) messageFactoryProvider.getFactoryByType(SupportedMessageListContentType.ADAMANT_BASIC);
            AdamantBasicMessage messageEntity = getAdamantMessage(text, messageFactory);

            long cost = messageFactory.getMessageProcessor().calculateMessageCostInAdamant(messageEntity);
            getViewState().showMessageCost(BalanceConvertHelper.convert(cost).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private AdamantBasicMessage getAdamantMessage(String message, AdamantBasicMessageFactory messageFactory) {
        AdamantBasicMessage abstractMessage = null;
        try {
            String publicKey = api.getAccount().getPublicKey();
            abstractMessage = messageFactory.getMessageBuilder().build(
                    null,
                    message, true,
                    System.currentTimeMillis(),
                    currentChat.getCompanionId(),
                    publicKey
            );
        } catch (Exception e) {
            e.printStackTrace();
            router.showSystemMessage(e.getMessage());
        }

        return abstractMessage;
    }


    public void onClickShowRenameDialog() {
        if (currentChat != null){
            if (currentChat.getCompanionId().equalsIgnoreCase(currentChat.getTitle())){
                getViewState().showRenameDialog(currentChat.getCompanionId());
            } else {
                getViewState().showRenameDialog(currentChat.getTitle());
            }
        }
    }

    public void onClickRenameButton(String newName) {
        if (currentChat != null){
            currentChat.setTitle(newName);

            getViewState().changeTitles(newName, currentChat.getCompanionId());
            getViewState().startSavingContacts();
        }
    }

    public void onClickSendCurrencyButton() {
        if (currentChat != null){
            router.navigateTo(Screens.SEND_CURRENCY_TRANSFER_SCREEN, currentChat.getCompanionId());
        }
    }
}
