package com.example.saferhike.api

import android.os.Parcelable
import android.util.Base64
import android.util.Log
import com.example.saferhike.viewModels.EmergencyContact
import com.example.saferhike.viewModels.HikeMarker
import com.example.saferhike.viewModels.HikeReq
import com.example.saferhike.viewModels.UserReq
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

class ApiService {
    private val _retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService: ApiRoutes by lazy {
        _retrofit.create(ApiRoutes::class.java)
    }
    private val _flaskPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnw/JsP/8vVvFN2+nmk4b" +
            "kgMpGoe2fiQfGqhhS95aiqZ3mvDqlzTPH3Rq9f+vqVSGV8PbUn5kloFfV/GXDmfQ" +
            "TvOKQOrU96oQKmJ3OJSGHBcB8hg1gXTtl6Jqa53GqKEDGpPmtR4JbZTkwtBP6cmx" +
            "Yhc5o2TbZLOzHZl+k0NTqaubyu4ifF6QrjRrfNsZobMQwMt7FQOmWdEdk4y4Ji4B" +
            "H8AXgWbkwvOrWQi7FUlpEtQpQ3u4e6J4ldsfoj9vjGUtlajk1swtu5S0MtUzrUDM" +
            "2O1wRvjNKwCKHkPWstH8Mf2f2qmjyG/DUM+MQFPtwqhX7Oh+PZ64FfX0p+3L8L57" +
            "kQIDAQAB"


    private fun getFlaskPublicKey(): PublicKey {
        val decodedKey = Base64.decode(_flaskPublicKey, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(decodedKey)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    private fun getPrivateKey(alias: String): PrivateKey {
        Log.d("ApiService", "Attempting to getPrivateKey")
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        return keyStore.getKey(alias, null) as PrivateKey
    }
    // Encrypts a given string using the provided public key
    private fun encryptData(data: String, publicKey: PublicKey): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }
    // Decrypts a given string using the provided private key
    private fun decryptData(encryptedData: String, privateKey: PrivateKey): String {
        Log.d("ApiService", "Decrypting data: $encryptedData")
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        Log.d("ApiService", "Initializing cipher")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        Log.d("ApiService", "Attempting to decode bytes and do final decryption")
        return try {
            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            String(cipher.doFinal(encryptedBytes))
        } catch (e: Exception) {
            ""
        }
    }
    // Encrypts a HikeReq object to EncryptedHikeReq using the provided public key
    fun encryptHikeReq(hikeReq: HikeReq): EncryptedHikeReq {
        val publicKey = getFlaskPublicKey()
        val encryptedLat = encryptData(hikeReq.lat.toString(), publicKey)
        val encryptedLng = encryptData(hikeReq.lng.toString(), publicKey)
        val encryptedMarkers = hikeReq.markers.map { marker ->
            EncryptedHikeMarker(
                encryptData(marker.lat.toString(), publicKey),
                encryptData(marker.lng.toString(), publicKey),
                encryptData(marker.title, publicKey),
                encryptData(marker.description, publicKey)
            )
        }
        val encryptedTraveledPath = hikeReq.traveledPath.map { latLng ->
            EncryptedLatLng(
                encryptData(latLng.latitude.toString(), publicKey),
                encryptData(latLng.longitude.toString(), publicKey)
            )
        }
        return EncryptedHikeReq(
            pid = hikeReq.pid,
            uid = encryptData(hikeReq.uid, publicKey),
            name = encryptData(hikeReq.name, publicKey),
            supplies = encryptData(hikeReq.supplies, publicKey),
            lat = encryptedLat,
            lng = encryptedLng,
            duration = encryptData(hikeReq.duration, publicKey),
            markers = encryptedMarkers,
            traveledPath = encryptedTraveledPath,
            completed = hikeReq.completed,
            inProgress = hikeReq.inProgress
        )
    }
    // Decrypts an EncryptedHikeReq object back to HikeReq using the provided private key
    fun decryptHikeReq(encryptedHikeReq: EncryptedHikeReq, alias: String): HikeReq {
        val privateKey = getPrivateKey(alias)
        Log.d("ApiService", "Private Key Received: $privateKey")
        Log.d("ApiService", "Encrypted Hike Req Received: $encryptedHikeReq")
        val decryptedLat = decryptData(encryptedHikeReq.lat, privateKey).toDouble()
        val decryptedLng = decryptData(encryptedHikeReq.lng, privateKey).toDouble()
        Log.d("ApiService", "Decrypting Markers")
        val decryptedMarkers = encryptedHikeReq.markers.map { marker ->
            HikeMarker(
                decryptData(marker.lat, privateKey).toDouble(),
                decryptData(marker.lng, privateKey).toDouble(),
                decryptData(marker.title, privateKey),
                decryptData(marker.description, privateKey)
            )
        }
        Log.d("ApiService", "Decrypting Traveled path")
        val decryptedTraveledPath = encryptedHikeReq.traveledPath.map { latLng ->
            LatLng(
                decryptData(latLng.latitude, privateKey).toDouble(),
                decryptData(latLng.longitude, privateKey).toDouble()
            )
        }
        Log.d("ApiService", "Returning HikeReq")
        Log.d("ApiService", encryptedHikeReq.duration)
        return HikeReq(
            pid = encryptedHikeReq.pid,
            uid = decryptData(encryptedHikeReq.uid, privateKey),
            name = decryptData(encryptedHikeReq.name, privateKey),
            supplies = decryptData(encryptedHikeReq.supplies, privateKey),
            lat = decryptedLat,
            lng = decryptedLng,
            duration = decryptData(encryptedHikeReq.duration, privateKey),
            markers = decryptedMarkers,
            traveledPath = decryptedTraveledPath,
            completed = encryptedHikeReq.completed,
            inProgress = encryptedHikeReq.inProgress
        )
    }

    // Encrypts a UserReq object to EncryptedUserReq using the provided public key
    fun encryptUserReq(userReq: UserReq): EncryptedUserReq {
        val publicKey = getFlaskPublicKey()

        val encryptedUid = encryptData(userReq.uid, publicKey)
        val encryptedPublicKey = userReq.publicKey
        val encryptedFName = encryptData(userReq.fName, publicKey)
        val encryptedLName = encryptData(userReq.lName, publicKey)
        val encryptedEmergencyContacts = userReq.emergencyContacts.map { contact ->
            EncryptedEmergencyContact(
                phoneNumber = encryptData(contact.phoneNumber, publicKey),
                email = encryptData(contact.email, publicKey)
            )
        }

        return EncryptedUserReq(
            uid = encryptedUid,
            publicKey = encryptedPublicKey,
            fName = encryptedFName,
            lName = encryptedLName,
            emergencyContacts = encryptedEmergencyContacts
        )
    }

    // Decrypts an EncryptedUserReq object back to UserReq using the provided private key
    fun decryptUserReq(encryptedUserReq: EncryptedUserReq, alias: String): UserReq {
        val privateKey = getPrivateKey(alias)
        Log.d("ApiService", "Private Key Received: $privateKey")
        val decryptedUid = decryptData(encryptedUserReq.uid, privateKey)
        Log.d("ApiService", "Decrypting uid: ${encryptedUserReq.uid}")
        val decryptedPublicKey = encryptedUserReq.publicKey
        Log.d("ApiService", "Decrypting FName and LName ${encryptedUserReq.fName} : ${encryptedUserReq.lName}")
        val decryptedFName = decryptData(encryptedUserReq.fName, privateKey)
        val decryptedLName = decryptData(encryptedUserReq.lName, privateKey)
        Log.d("ApiService", "Attempting to handle emergencyContacts")
        val decryptedEmergencyContacts = encryptedUserReq.emergencyContacts.map { encryptedContact ->
            EmergencyContact(
                phoneNumber = decryptData(encryptedContact.phoneNumber, privateKey),
                email = decryptData(encryptedContact.email, privateKey)
            )
        }

        return UserReq(
            uid = decryptedUid,
            publicKey = decryptedPublicKey,
            fName = decryptedFName,
            lName = decryptedLName,
            emergencyContacts = decryptedEmergencyContacts
        )
    }

    fun decryptPath(encryptedPath: List<EncryptedLatLng>, alias: String): List<LatLng> {
        val privateKey = getPrivateKey(alias)
        return encryptedPath.map {
            LatLng(
                decryptData(it.latitude, privateKey).toDouble(),
                decryptData(it.longitude, privateKey).toDouble()
            )
        }
    }
}
@Parcelize
data class EncryptedHikeReq(
    val pid: Int,
    val uid: String, // Encrypted UID
    val name: String, // Encrypted Name
    val supplies: String, // Encrypted Supplies
    val lat: String, // Encrypted Latitude as String
    val lng: String, // Encrypted Longitude as String
    val duration: String, // Encrypted Expected Return Time
    val markers: List<EncryptedHikeMarker>, // List of Encrypted Markers
    val traveledPath: List<EncryptedLatLng>, // List of Encrypted LatLng for Traveled Path
    var completed: Boolean = false,
    var inProgress: Boolean = false
) : Parcelable

@Parcelize
data class EncryptedHikeMarker(
    val lat: String, // Encrypted Latitude as String
    val lng: String, // Encrypted Longitude as String
    val title: String, // Encrypted Title
    val description: String // Encrypted Description
) : Parcelable

@Parcelize
data class EncryptedLatLng(
    val latitude: String, // Encrypted Latitude as String
    val longitude: String // Encrypted Longitude as String
) : Parcelable

@Parcelize
data class EncryptedUserReq(
    val uid: String,
    val publicKey: String,
    val fName: String,
    val lName: String,
    val emergencyContacts: List<EncryptedEmergencyContact>
) : Parcelable

@Parcelize
data class EncryptedEmergencyContact(
    val phoneNumber: String,
    val email: String
) : Parcelable
