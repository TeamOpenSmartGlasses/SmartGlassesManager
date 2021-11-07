from Crypto.Cipher import AES
import binascii
import hashlib

class AESpy:
    def __init__(self):
        pass

    def make_key(self, key):
        hash_object = hashlib.sha1(key.encode("utf-8"))
        hash_key = hash_object.digest()[:16]
        return hash_key

    def encrypt(self, plain_text_bytes, key):
        hash_key = self.make_key(key)
        cipher = AES.new(hash_key, AES.MODE_ECB)
        cipher_bytes = cipher.encrypt(plain_text_bytes)
        return cipher_bytes

    def decrypt(self, cipher_bytes, key):
        hash_key = self.make_key(key)
        decipher = AES.new(hash_key, AES.MODE_ECB)
        plain_text_bytes = decipher.decrypt(cipher_bytes)
        return plain_text_bytes

