package com.orange.keymanager.grpc

import com.orange.keymanager.KeyManagerRequest
import com.orange.keymanager.KeyManagerResponse
import com.orange.keymanager.KeyManagerServiceGrpc
import com.orange.keymanager.models.*
import com.orange.keymanager.rest.ItauErpRestClient
import io.grpc.Status.*
import io.grpc.stub.StreamObserver
import javax.inject.Singleton

@Singleton
class CreateKeyGrpcServer(
    private val itauErpRestClient: ItauErpRestClient,
    private val pixClientRepository: PixClientRepository) : KeyManagerServiceGrpc.KeyManagerServiceImplBase() {

    override fun createKey(request: KeyManagerRequest, responseObserver: StreamObserver<KeyManagerResponse>?) {

        val convertedKeyType = request.getConvertedKeyType()
        val convertedAccountType = request.getConvertedAccountType()

        if(!convertedKeyType.isValid(request.keyValue)) {
            GrpcRuntimeError.throwError(INVALID_ARGUMENT, "Invalid key value", responseObserver)
        }

        if(convertedKeyType != KeyType.RANDOM) {
            if (pixClientRepository.findByKeyValue(request.keyValue).isPresent) {
                GrpcRuntimeError.throwError(ALREADY_EXISTS, "Pix key already registered", responseObserver)
            }
        }

        try {
            itauErpRestClient.findByClientId(request.clientId)
        } catch(exception: Exception) {
            GrpcRuntimeError.throwError(NOT_FOUND, "Client ID not exists", responseObserver)
        }

        val keyValue = GenerateKeyValue.generate(
            keyType = convertedKeyType,
            keyValue = request.keyValue
        )

        val pixClientKey = PixClientKey(
            clientId = request.clientId,
            keyValue = keyValue,
            keyType = convertedKeyType,
            accountType = convertedAccountType
        )

        val savedPixClientKey = pixClientRepository.save(pixClientKey)

        val response = KeyManagerResponse.newBuilder()
            .setKeyId(savedPixClientKey.id!!)
            .setKeyValue(savedPixClientKey.keyValue)
            .setKeyType(request.keyType)
            .setAccountType(request.accountType)
            .build()

        responseObserver!!.onNext(response)
        responseObserver.onCompleted()
    }

    private fun KeyManagerRequest.getConvertedKeyType() : KeyType {
        return KeyType.valueOf(this.keyType.toString())
    }

    private fun KeyManagerRequest.getConvertedAccountType() : AccountType {
        return AccountType.valueOf(this.accountType.toString())
    }
}