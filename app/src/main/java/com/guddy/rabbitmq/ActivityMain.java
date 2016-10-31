package com.guddy.rabbitmq;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.TextView;

import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ActivityMain extends AppCompatActivity {

    //region Constants
    private static final String HOST = "10.0.3.2"; // host when using GenyMotion
    private static final String EXCHANGE_NAME = "lives";
    private static final String FORMAT = "hh:mm:ss";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(FORMAT);
    //endregion

    //region Fields
    private Fanout mFanout;
    private Subscription mSubscription;
    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
        try {
            mFanout.close();
        } catch (IOException pE) {
            pE.printStackTrace();
        }
    }
    //endregion

    //region Specific job
    private void subscribe() {
        mSubscription = rxSubscribe()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(final Throwable poThrowable) {
                        poThrowable.printStackTrace();
                    }

                    @Override
                    public void onNext(final String psMessage) {
                        appendMessage(psMessage);
                    }
                });
    }

    private Observable<String> rxSubscribe() {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> pSubscriber) {
                try {
                    mFanout = new Fanout(HOST, EXCHANGE_NAME);

                    final String lsQueue = mFanout.getChannel().queueDeclare().getQueue();
                    mFanout.getChannel().queueBind(lsQueue, mFanout.exchangeName, "");

                    final QueueingConsumer loQueueingConsumer = new QueueingConsumer(mFanout.getChannel());
                    mFanout.getChannel().basicConsume(lsQueue, true, loQueueingConsumer);

                    while (!mSubscription.isUnsubscribed()) {
                        final QueueingConsumer.Delivery loDelivery = loQueueingConsumer.nextDelivery();
                        final String lsMessage = new String(loDelivery.getBody());
                        pSubscriber.onNext(lsMessage);
                    }
                } catch (final IOException | TimeoutException | InterruptedException pException) {
                    pSubscriber.onError(pException);
                }
            }
        });
    }

    private void appendMessage(@NonNull final String psMessage) {
        final TextView loTextView = (TextView) findViewById(R.id.ActivityMain_TextView);
        String lsText = loTextView.getText().toString();
        final Date loNow = new Date();
        final String lsTextToAdd = SIMPLE_DATE_FORMAT.format(loNow) + ' ' + psMessage + '\n';
        if (TextUtils.isEmpty(lsText)) {
            lsText = lsTextToAdd;
        } else {
            lsText = lsTextToAdd.concat(lsText);
        }
        loTextView.setText(lsText);
    }
    //endregion
}
