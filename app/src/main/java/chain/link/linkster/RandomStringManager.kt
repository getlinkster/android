package chain.link.linkster
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import java.security.SecureRandom
import java.util.Random

object RandomStringManager {
    private const val RANDOM_STRING_KEY = "random_string_key"

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    val randomString: String
        get() {
            val savedRandomString = sharedPreferences.getString(RANDOM_STRING_KEY, null)
            return if (savedRandomString != null) {
                savedRandomString
            } else {
                val newRandomString = generateRandomString()
                saveRandomString(newRandomString)
                newRandomString
            }
        }

    private fun generateRandomString(): String {
        val charArray = "some secret table nop fff so GJ".toCharArray()
        val random = Random()

        for (i in charArray.indices) {
            val randomIndex = random.nextInt(charArray.size)
            val temp = charArray[i]
            charArray[i] = charArray[randomIndex]
            charArray[randomIndex] = temp
        }

        return String(charArray)
//        return "some secret table nop fff so GJ"
    }

    private fun saveRandomString(randomString: String) {
        val editor = sharedPreferences.edit()
        editor.putString(RANDOM_STRING_KEY, randomString)
        editor.apply()
    }
}