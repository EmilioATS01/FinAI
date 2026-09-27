package emilio.tolosa.finai.networking.models

import androidx.lifecycle.MutableLiveData

class ChatModel {
    val respuesta = MutableLiveData<String>()
    val error = MutableLiveData<String>()
}