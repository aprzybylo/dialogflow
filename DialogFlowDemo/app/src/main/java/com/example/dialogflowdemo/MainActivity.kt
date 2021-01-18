package com.example.dialogflowdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.example.dialogflowdemo.ui.main.MovieViewModel
import com.example.dialogflowdemo.ui.main.adapters.SectionsPagerAdapter
import com.example.dialogflowdemo.ui.main.http.RequestTask
import com.example.dialogflowdemo.ui.main.http.model.DialogFlowResponse
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2beta1.DetectIntentResponse
import com.google.cloud.dialogflow.v2beta1.QueryInput
import com.google.cloud.dialogflow.v2beta1.SessionName
import com.google.cloud.dialogflow.v2beta1.SessionsClient
import com.google.cloud.dialogflow.v2beta1.SessionsSettings
import com.google.cloud.dialogflow.v2beta1.TextInput
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import java.util.Locale
import java.util.stream.Collectors

class MainActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val context = this

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermission()
        }

        var micButton: FloatingActionButton = findViewById<FloatingActionButton>(R.id.button)
        var speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(bytes: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onError(i: Int) {
            }

            override fun onResults(bundle: Bundle) {
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                data?.let {
                    onTextRecognized(data[0])
                    performDialogFlowQuery(data[0])
                }
            }

            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle) {}
        }
        )

        var isListeningEnabled = true

        micButton.setOnClickListener(
            View.OnClickListener {
                if (isListeningEnabled == false) {
                    micButton.setImageResource(R.drawable.ic_mic)
                    speechRecognizer.stopListening()
                } else {
                    micButton.setImageResource(R.drawable.ic_hearing)
                    speechRecognizer.startListening(speechRecognizerIntent)
                }
                isListeningEnabled = !isListeningEnabled
            }
        )
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), MainActivity.RECORD_AUDIO_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MainActivity.RECORD_AUDIO_REQUEST_CODE && grantResults.size > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun onTextRecognized(text: String) {
        var micButton: FloatingActionButton = findViewById<FloatingActionButton>(R.id.button)
        var infoText: TextView = findViewById<TextView>(R.id.infoText)
        micButton.setImageResource(R.drawable.ic_mic)
        infoText.setText(text)
    }

    fun performDialogFlowQuery(text: String) {
        try {
            val stream = resources.openRawResource(R.raw.agent_credentials)
            val credentials = GoogleCredentials.fromStream(stream)
            val projectId = (credentials as ServiceAccountCredentials).projectId

            val settingsBuilder = SessionsSettings.newBuilder()
            val sessionsSettings =
                settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build()
            var client: SessionsClient? = SessionsClient.create(sessionsSettings)
            var session: SessionName? = SessionName.of(projectId, "someSessionName")

            val queryInput =
                QueryInput.newBuilder().setText(TextInput.newBuilder().setText(text).setLanguageCode(LANGUAGE_CODE)).build()
            RequestTask(this@MainActivity, session!!, client!!, queryInput).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onResult(response: DetectIntentResponse?) {
        var infoText: TextView = findViewById<TextView>(R.id.infoText)

        try {
            if (response != null) {

                var dialogFlowResponse = response.mapToDialogFlowResponse()
                performAction(dialogFlowResponse)

            } else {
                infoText.setText("Response is empty")
            }
        } catch (e: Exception) {
            infoText.setText("Error: " + e.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun DetectIntentResponse.mapToDialogFlowResponse(): DialogFlowResponse {
        var params = queryResult.parameters.fields

        return DialogFlowResponse(
            queryText = queryResult.queryText,
            action = queryResult.action,
            parameters = params.map { it.key to it.value.stringValue }.toMap(),
            fulfillmentText = queryResult.fulfillmentText
        )
    }

    fun performAction(dialogFlowResponse: DialogFlowResponse) {
        Log.d("olabola", dialogFlowResponse.toString())

        when (dialogFlowResponse.action) {
            "character" -> {
                getCharacterAction(dialogFlowResponse)
            }
            "duration" -> {
                getDurationAction(dialogFlowResponse)
            }
            "seasons" -> {
                getSeasonAction(dialogFlowResponse)
            }
            "fragment" -> {
                changeFragmentAction(dialogFlowResponse)
            }
        }
    }

    private fun changeFragmentAction(dialogFlowResponse: DialogFlowResponse) {
        var fragmentName = dialogFlowResponse.parameters?.getValue("fragment")
        when(fragmentName){
            "Settings" -> {
                viewPager.setCurrentItem(2, true)
            }
            "MyFavourites" -> {
                viewPager.setCurrentItem(1, true)
            }
            "Home" ->{
                viewPager.setCurrentItem(0, true)
            }
        }
    }

    private fun getSeasonAction(dialogFlowResponse: DialogFlowResponse) {
        var infoText: TextView = findViewById<TextView>(R.id.infoText)

        var movieTitle = dialogFlowResponse.parameters?.getValue("title")
        var movie: MovieViewModel? = null

        for (movieHolder in MOVIES_LIST) {
            if (movieHolder.title.equals(movieTitle)) {
                movie = movieHolder
            }
        }

        movie?.let {
            if (movie.sesonsNumber == null) {
                infoText.setText("It is a movie and it doesn't have any seasons!")
            } else {
                infoText.setText("Season number is " + movie.sesonsNumber)
            }
        } ?: infoText.setText("Movie was not found")
    }

    private fun getDurationAction(dialogFlowResponse: DialogFlowResponse) {
        var infoText: TextView = findViewById<TextView>(R.id.infoText)

        var durationTime = dialogFlowResponse.parameters?.getValue("title")
        var movie: MovieViewModel? = null

        for (movieHolder in MOVIES_LIST) {
            if (movieHolder.title.equals(durationTime)) {
                movie = movieHolder
            }
        }

        movie?.let {
            if (movie.duration.isNullOrEmpty()) {
                infoText.setText("Duration time is unknown")
            } else {
                infoText.setText("Duration time is " + movie?.duration + " minutes.")
            }
        } ?: infoText.setText("Movie was not found")
    }

    private fun getCharacterAction(dialogFlowResponse: DialogFlowResponse) {
        var infoText: TextView = findViewById<TextView>(R.id.infoText)

        var movieTitle = dialogFlowResponse.parameters?.getValue("title")
        var movie: MovieViewModel? = null

        for (movieHolder in MOVIES_LIST) {
            if (movieHolder.title.equals(movieTitle)) {
                movie = movieHolder
            }
        }

        movie?.let {
            if (movie.cast.isNullOrEmpty()) {
                infoText.setText("Main cast is unknown")
            } else {
                infoText.setText("Main cast is " + movie?.cast)
            }
        } ?: infoText.setText("Movie was not found")
    }

    companion object {

        const val RECORD_AUDIO_REQUEST_CODE = 1
        const val LANGUAGE_CODE = "en"

        /**
         * The list that represents movies and series
         */
        val MOVIES_LIST = listOf<MovieViewModel>(
            MovieViewModel(
                "Titanic",
                "A seventeen-year-old aristocrat falls in love with a kind but poor artist aboard the luxurious, ill-fated R.M.S. Titanic.",
                "195",
                "https://fwcdn.pl/fph/01/87/187/954724_1.1.jpg",
                "Leonardo DiCaprio, Kate Winslet",
                null
            ),
            MovieViewModel(
                "The Big Bang Theory",
                "A woman who moves into an apartment across the hall from two brilliant but socially awkward physicists shows them how " +
                    "little they know about life outside of the laboratory.",
                "22",
                "https://cdn.hbogo.eu/images/89645622-A85E-4161-9D3B-CD0B37F2599E/1080_463.jpg",
                //"Johnny Galecki, Jim Parsons, Kaley Cuoco",
                null,
                12
            ),
            MovieViewModel(
                "Home Alone",
                "An eight-year-old troublemaker must protect his house from a pair of burglars when he is accidentally " +
                    "left home alone by his family during Christmas vacation.",
                "103",
                "https://cdn.flickeringmyth.com/wp-content/uploads/2018/12/home-alone-2.jpg",
                "Macaulay Culkin, Joe Pesci, Daniel Stern",
                null
            )
        )
    }
}