package com.example.dialogflowdemo.ui.main.http.model

class DialogFlowResponse (

    var queryText: String?,

    var action: String?,

    var parameters: Map<String,String>?,

    var fulfillmentText: String?


) {
    override fun toString(): String {
        return "DialogFlowResponse(queryText=$queryText, action=$action, parameters=$parameters, fulfillmentText=$fulfillmentText)"
    }
}

