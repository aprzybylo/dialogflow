package com.example.dialogflowdemo.ui.main.http

import android.app.Activity
import android.os.AsyncTask
import com.example.dialogflowdemo.MainActivity
import com.google.cloud.dialogflow.v2beta1.DetectIntentRequest
import com.google.cloud.dialogflow.v2beta1.DetectIntentResponse
import com.google.cloud.dialogflow.v2beta1.QueryInput
import com.google.cloud.dialogflow.v2beta1.SessionName
import com.google.cloud.dialogflow.v2beta1.SessionsClient

class RequestTask : AsyncTask<Void, Void, DetectIntentResponse> {

    var activity: Activity? = null
    private var session: SessionName? = null
    private var sessionsClient: SessionsClient? = null
    private var queryInput: QueryInput? = null


    constructor(activity: Activity,session:SessionName,sessionsClient: SessionsClient,queryInput: QueryInput){
        this.activity=activity
        this.session=session
        this.queryInput=queryInput
        this.sessionsClient=sessionsClient
    }

    override fun doInBackground(vararg params: Void?): DetectIntentResponse? {
        try {
            val detectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(session.toString())
                .setQueryInput(queryInput)
                .build()
            return sessionsClient?.detectIntent(detectIntentRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        return null
    }

    override fun onPostExecute(result: DetectIntentResponse?) {
        (activity as MainActivity).onResult(result)
    }

}