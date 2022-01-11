package com.google.mediapipe.apps.wearableai.database.person;

//originally from MXT: Memory Expansion Tools
//Jeremy Stairs (stairs1) and Cayden Pierce
//https://github.com/stairs1/memory-expansion-tools

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.Date;

import java.util.List;

import com.google.mediapipe.apps.wearableai.database.phrase.Phrase;

//some of the ways we are structuring this may not be best practices for relational databases (argKey and argValue, for one). However, in the name of not having a 100s of files for every table, we've abstracted a bit here and tried to make it a bit broader. Perhaps this is ok, or perhaps we'll redesign once we have a better idea of all the data we're actually going to need to store

//this one is abstract class instead of interface so we can use @Transaction

//new_face_name - the first time we have seen this face, 
    //value is the person's name, 
    //"unknown" if not known by the system, 
    //"confirmed_unknown" if the user also doesn't know who they are
    //"changed" if the person id was changed to a new one - the argValue is the id of the new personId cast to a string - this is inefficient, but allows us to stick to one table, and this should only be rarely used as the face rec api will update its internal memory after one hit 
    //
//event - when someone does something
//meta - a descriptor of something - for example "meta" : "bad_for_face_rec" means the image is not good enough to run facial recognition with
//
//
//PersonCreator.create(null, "meta", "bad_for_face_rec", timestamp, mediaId, mPersonRepository);
//for this, the personId is the original personId - the row id of the "new_face_name" row

@Dao
public abstract class PersonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insert(PersonEntity person);

    //set the personId to the row id
    @Query("UPDATE PersonTable SET personId = :personId WHERE id = :personId")
    abstract void updatePersonId(long personId);

    //set the unknownPersonId to the to the given knownPersonid
    @Query("UPDATE PersonTable SET personId = :knownPersonId WHERE personId = :unknownPersonId")
    abstract void changePersonIdSubQuery(long unknownPersonId, long knownPersonId);

    //remove the 'new_face_name' row - should only be used as part of 'changePersonId' - or it will break things
    @Query("UPDATE PersonTable SET argKey = 'changed', argValue = :newPersonIdString WHERE personId = :unknownPersonId AND argKey = 'new_face_name'")
    abstract void changeNewFaceName(long unknownPersonId, String newPersonIdString);

   @Transaction
   void changePersonId(long unknownPersonId, long knownPersonId) {
     String knownPersonIdString = Long.toString(knownPersonId);
     changeNewFaceName(unknownPersonId, knownPersonIdString);
     changePersonIdSubQuery(unknownPersonId, knownPersonId);
   }

   //get references to a person that was changed/updated
   @Query("SELECT * from PersonTable WHERE argKey = 'changed' AND id = :rowId LIMIT 1")
   abstract PersonEntity getChangedPersonId(long rowId);

    //set memory of person as deleted
    @Query("UPDATE PersonTable SET argValue = 'deleted' WHERE personId = :personId AND argKey = 'new_face_name'")
    abstract void setPersonDeleted(long personId);

    //delete a person, but keep the reference to their name so we don't create another person with the same id
    @Query("DELETE FROM PersonTable WHERE personId = :personId AND argKey != 'new_face_name'")
    abstract void deletePersonInit(long personId);

    //delete a person, but keep the reference to their name so we don't create another person with the same id
   @Transaction
   void deletePerson(long personId) {
     deletePersonInit(personId);
     setPersonDeleted(personId);
   }

    //update the person's name
    @Query("UPDATE PersonTable SET argValue = :name WHERE id = :personId AND argKey = 'new_face_name'")
    abstract void updatePersonName(String name, long personId);

    @Query("DELETE FROM PersonTable")
    abstract void deleteAll();

    @Query("SELECT * from PersonTable ORDER BY timestamp DESC")
    abstract LiveData<List<PersonEntity>> getAllPersons();

    @Query("SELECT * from PersonTable WHERE argKey = 'meta' AND argValue = 'bad_for_face_rec' ORDER BY timestamp DESC")
    abstract LiveData<List<PersonEntity>> getBadForFaceRec();

    @Query("SELECT * from PersonTable ORDER BY timestamp DESC")
    abstract List<PersonEntity> getAllPersonsSnapshot();
    
    @Query("SELECT * from PersonTable WHERE argKey = 'new_face_name' AND (argValue != 'unknown') AND (argValue != 'confirmed_unknown') ORDER BY timestamp DESC")
    abstract List<PersonEntity> getAllKnownPersonsNamesSnapshot();
    
    @Query("SELECT * from PersonTable WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    abstract List<PersonEntity> getPersonRange(Date startTime, Date endTime);

    @Query("SELECT * FROM PersonTable WHERE ID = :id")
    abstract LiveData<PersonEntity> get_by_id(int id);

    @Query("SELECT * FROM PersonTable WHERE ID = :id AND argKey = 'new_face_name' ORDER BY timestamp DESC LIMIT 1") //should always be only one entry anyway, but if we mess up, get the latest name
    abstract PersonEntity getPersonsName(long id);

    @Query("SELECT * FROM PersonTable WHERE ID = :id AND argKey= 'event' AND argValue = 'seen' ORDER BY timestamp DESC LIMIT 1") //the last time we saw this person
    abstract PersonEntity getPersonLastSeen(long id);

    @Query("SELECT * FROM PersonTable WHERE argKey = 'new_face_name' AND argValue = 'unknown' ORDER BY timestamp DESC")
    abstract List<PersonEntity> getUnknownPersonsSnapshot();

}
