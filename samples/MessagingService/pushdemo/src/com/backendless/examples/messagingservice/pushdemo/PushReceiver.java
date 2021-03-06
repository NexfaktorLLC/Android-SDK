package com.backendless.examples.messagingservice.pushdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import com.backendless.messaging.PublishOptions;
import com.backendless.push.BackendlessBroadcastReceiver;

public class PushReceiver extends BackendlessBroadcastReceiver
{
  @Override
  public void onMessage( Context context, Intent intent )
  {
    if( PushActivity.handler != null )
    {
      Message message = new Message();
      message.obj = intent.getStringExtra( PublishOptions.MESSAGE_TAG );
      PushActivity.handler.sendMessage( message );
    }
  }

  @Override
  public void onError( Context context, String messageError )
  {
    if( PushActivity.handler != null )
    {
      Message message = new Message();
      message.obj = new Error( messageError );
      PushActivity.handler.sendMessage( message );
    }
  }
}