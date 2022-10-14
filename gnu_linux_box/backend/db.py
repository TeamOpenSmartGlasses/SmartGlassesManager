from pymongo import MongoClient
from bson.objectid import ObjectId

from datetime import datetime
import time

#originally from https://github.com/stairs1/memory-expansion-tools

class Database:
    def __init__(self, mongoAddress="mongodb://127.0.0.1:27017"):
        self.mongoAddress = mongoAddress

    def connect(self):
        # connect to MongoDB
        mongo = MongoClient(self.mongoAddress)
        db = mongo.ossg
        self.db = db

    def getStatus(self):
        # Issue the serverStatus command and print the results
        serverStatusResult = self.db.command("serverStatus")
        return serverStatusResult

    def addUser(self, name, username, email, password):
        userId = self.nameToId(username)
        if self.userExists(userId):
            return None
        usersCollection = self.db.users
        stagesCollection = self.db.l1stages #TODO this is too dependent on the l1 stage right now, 
        user = {"name": name, "email": email, "timestamp": time.time(), "password": password, "username": username}
        resp = usersCollection.insert_one(user)
        userId = resp.inserted_id
        empty = {"userId" : userId, "stage" : { "1" : None, "2" : None, "3" : None, "4" : None }}
        stagesCollection.insert_one(empty)
        return userId

    def addTag(self, username, tag):
        userId = self.nameToId(username)
        if not self.userExists(userId):
            return None
        tagsCollection = self.db.tags
        tagJson = {"username": username, "tag" : tag, "timestamp" : time.time()}
        resp = tagsCollection.insert_one(tagJson)
        if resp:
            return True

    def removeTag(self, username, tag):
        userId = self.nameToId(username)
        if not self.userExists(userId):
            return None
        tagsCollection = self.db.tags
        tagsCollection.delete_one({"username" : username, "tag" : tag})
        return True

    def getTags(self, username):
        userId = self.nameToId(username)
        if not self.userExists(userId):
            return None
        tagsCollection = self.db.tags
        resp = tagsCollection.find({"username" : username})
        respl = list()
        for i in resp:
            respl.append(i["tag"])
        return respl

    def nameToId(self, username):
        usersCollection = self.db.users
        _id = usersCollection.find_one({"username": username})
        if not _id:
            return False
        return _id["_id"]

    def getPass(self, username):
        userId = self.nameToId(username)

        if not userId:
            return None

        if not self.userExists(userId):
            return None
        else:
            usersCollection = self.db.users
            apass = usersCollection.find_one({"_id": ObjectId(userId)})["password"]
            return apass

    def addTalk(self, userId, words, timestamp, latitude, longitude, address, cache=0):
        if not self.userExists(userId):
            return "No such user exists, exiting"  # TODO throw error

        if cache == 2:
            talksCollection = self.db.ltwotalks
        elif cache == 3:
            talksCollection = self.db.lthreetalks
        elif cache == -1:
            talksCollection = self.db.annotate
        elif cache == -2:
            talksCollection = self.db.todo
        else:
            talksCollection = self.db.talks

        talk = {"userId": str(userId), "talk": words, "timestamp": timestamp, "latitude" : latitude, "longitude" : longitude, "address" : address}

        if talk in talksCollection.find(
            {}, {"userId": 1, "talk": 1, "timestamp": 1, "_id": 0}
        ):
            return -1

        resp = talksCollection.insert_one(talk)
        talkId = resp.inserted_id
        return talkId

    def talkExists(self, talkId: str):
        talksCollection = self.db.talks
        talkId = ObjectId(talkId)
        resp = talksCollection.find_one({"_id": talkId})

        if resp is None:
            return False
        else:
            return True

    def userExists(self, userId):
        usersCollection = self.db.users
        try:
            resp = usersCollection.find_one({"_id": ObjectId(userId)})
        except Exception as e:  # almost certainly means the userid is not a valid "ObjectId" bson type
            return False

        if resp is None:
            return False
        else:
            return True

    def search(self, userId, query, queryTime=None, timeRange=86400):
        talksCollection = self.db.talks

        if queryTime is None:
            startTime = 0
            endTime = time.time()  # we can't have memories from the future... yet
        else:
            startTime = queryTime - timeRange
            endTime = queryTime + timeRange

        resp = talksCollection.find(
            {
                "timestamp": {"$gt": startTime, "$lt": endTime},
                "userId": userId,
                "$text": {"$search": query},
            }
        ).sort("timestamp", -1)

        data = list()
        for item in resp:
            data.append(item)
        return data

    def insertTranscript(self, userId, transcript, timestamp):
        talksCollection = self.db.talks

        resp = talksCollection.insert_one({"transcript" : transcript, "userId" : userId, "timestamp" : timestamp})
        talkId = resp.inserted_id
        return talkId

    def getMostRecent(self, userId, num=1):
        if not self.userExists(userId):
            print("fail")
            return None

        talksCollection = self.db.talks

        resp = (
            talksCollection.find({"userId": str(userId)})
            .sort([("timestamp", -1)])
            .limit(num)
        )

        recents = list()
        for item in resp:  # little weird b/c resp is a pymongo-cursor
            item.pop("_id")
            recents.append(item)
        return recents

    def getRange(self, userId, num, start, end):
        if not self.userExists(userId):
            return None

        talksCollection = self.db.talks

        resp = (
                talksCollection.find({"userId": str(userId), "timestamp" : { "$gt" : start}, "timestamp" : { "$lt" : end}})
            .sort([("timestamp", -1)])
            .limit(num)
        )

        recents = list()
        for item in resp:  # little weird b/c resp is a pymongo-cursor
            item.pop("_id")
            recents.append(item)
        return recents


    def getPhrases(self, username, num=100, startDate=None, endDate=None):
        userId = self.nameToId(username)
        if not self.userExists(userId):
            return None

        if startDate is not None and endDate is not None:
            talks = self.getRange(userId, num, startDate, endDate)
        else:
            talks = self.getMostRecent(userId, num)
        phrases = list()
        for item in talks:
            phrases.append(item)
        return phrases

    def timeFlow(self, userId, talkId=None, timeFrame=None):
        talksCollection = self.db.talks

        if not self.userExists(userId):
            return None, None
        if talkId is None:  # if no talkId is give, use the most recent talk
            mostRecent = self.getMostRecent(userId)
            talkId = str(mostRecent[0]["_id"])
            reqTime = mostRecent[0]["timestamp"]
        elif not self.talkExists(
            talkId
        ):  # if talkId is given, only proceed if it actually exists in database
            return None, None
        else:  # if talksId is given to us AND it exists, get its time
            reqTime = talksCollection.find_one(
                {"_id": ObjectId(talkId)}, {"timestamp": 1, "_id": 0}
            )["timestamp"]

        if timeFrame is None or timeFrame == -1:
            timeFrame = 30

        startTime = reqTime - timeFrame
        endTime = reqTime + timeFrame

        resp = talksCollection.find(
            {"timestamp": {"$gt": startTime, "$lt": endTime}, "userId": userId}
        ).sort([("timestamp", 1)])

        data = list()
        for item in resp:
            data.append(item)

        return data, talkId

def main():
    db = Database()
    db.connect()
    print("hello")
    a = db.getTags("cayden")
    print(a)

if __name__ == "__main__":
    main()
