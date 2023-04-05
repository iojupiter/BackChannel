defmodule MongoInterface do

 def start_link do
   #{:ok, conn} = Mongo.start_link(database: "mongoTest")
   Mongo.start_link(name: :mongo, database: "BackChannel", pool: DBConnection.Poolboy)

 end

#ADMINISTRATION#
 def userIndex() do
   Mongo.command(:mongo, 
    %{createIndexes: "BackChannel_index", indexes: [ %{ key: %{ "username": 1 }, 
    name: "usernameIndex", unique: true} ] }, 
    pool: DBConnection.Poolboy)
 end
#ADMINISTRATION#

##USER MANAGEMENT##
def createUser(username, username_hash, token, screen) do
  Mongo.insert_one(:mongo, "Users", %{
    "username" => username,
    "username_hash" => username_hash,
    "token" => token,
    "friends" => [%{}],
    "friend_requests" => [],
    "screen_size" => screen}, pool: DBConnection.Poolboy)
end

def userExists(username_hash) do
  isContained = Mongo.find_one(:mongo, "Users", %{"username_hash" => username_hash}, pool: DBConnection.Poolboy)
  case isContained do
    %{"_id" => _id, "username_hash" => _username_hash} -> true
    nil -> false
  end
end

def searchUserbase(character) do
   users = Mongo.find(:mongo, "Users",
   %{"username" => %BSON.Regex{pattern: "^"<>character, options: ""}},
   projection: %{"username": 1, _id: 0}, 
   pool: DBConnection.Poolboy)
   Enum.map(users, fn(x) -> Map.values(x) |> List.to_string end)
   #finalList = allUsersMatching -- [username]
end

def deleteUser(username) do
  Mongo.delete_one(:mongo, "Users", 
    %{"username" => username }, pool: DBConnection.Poolboy)
end

def getScreenSize(username_hash) do
   ss = Mongo.find_one(:mongo, "Users",
    %{"username_hash" => username_hash},
     pool: DBConnection.Poolboy)
   ss["screen_size"]
end
##USER MANAGEMENT##



##FRIEND MANAGEMENT##
def isMemberFriend_RequestList(username, friend) do
    user = Mongo.find_one(:mongo, "Users",
   %{"username" => friend },
    pool: DBConnection.Poolboy)
    if Enum.member?(user["friend_requests"], username) do
      IO.inspect true
    else
      IO.inspect false
    end
end

def addFriendRequest(username, friend) do
    user = Mongo.find_one(:mongo, "Users",
   %{"username" => friend },
    pool: DBConnection.Poolboy)

    newFriend_requestList = user["friend_requests"] ++ [username]

    case MongoInterface.isMemberFriend_RequestList(username, friend) do
      false -> Mongo.update_one(:mongo, "Users",
                      %{"username" => friend},
                      %{"$set" => %{ "friend_requests" => newFriend_requestList }},
                      pool: DBConnection.Poolboy)
                      IO.inspect true
      true -> IO.inspect false
    end
end


def deleteFriend(username, friend) do
  #remove the friend from username friendlist
    newFriendMap = MongoInterface.getFriendsMap(username) |> Map.delete(friend)

    Mongo.update_one(:mongo, "Users",
                      %{"username" => username},
                      %{"$set" => %{ "friends" => [newFriendMap] }},
                      pool: DBConnection.Poolboy)

  #check if username is in friends friend_request list --> delete if so
    case MongoInterface.isMemberFriend_RequestList(username, friend) do
      false -> IO.inspect "friend deleted"

      true ->  user = Mongo.find_one(:mongo, "Users",
                                      %{"username" => friend },
                                        pool: DBConnection.Poolboy)
               newFriend_requestList = List.delete(user["friend_requests"], username)
               Mongo.update_one(:mongo, "Users",
                      %{"username" => friend},
                      %{"$set" => %{ "friend_requests" => newFriend_requestList }},
                      pool: DBConnection.Poolboy)
        IO.inspect "friend deleted"
    end
end


def updateFriendsList(username, profile) do
  {:ok, newFriendMap} = Poison.decode(profile)

  newFriendMap = 
  Enum.reduce(newFriendMap, %{}, fn {k, _v}, out ->
      if(Enum.member?(Map.keys(MongoInterface.getFriendsMap(k)), username) == true) do
        IO.inspect "#{k} exists"
        Map.put(out, k, "yes")
      else
        IO.inspect "#{k} does not exist"
        Map.put(out, k, "no")
      end
  end)

  Mongo.update_one(:mongo, "Users",
                      %{"username" => username},
                      %{"$set" => %{ "friends" => [newFriendMap] }},
                      pool: DBConnection.Poolboy)
  IO.puts "profile updated"
end

def getFriendsMap(username) do
  user = Mongo.find_one(:mongo, "Users",
   %{"username" => username },
    pool: DBConnection.Poolboy)
  List.first(user["friends"])
end

def getFriendRequests(username) do
    user = Mongo.find_one(:mongo, "Users",
   %{"username" => username },
    pool: DBConnection.Poolboy)
    user["friend_requests"]
end

def removeInFRL(username, friend) do
    user = Mongo.find_one(:mongo, "BackChannel",
   %{"username" => username },
    pool: DBConnection.Poolboy)
  
    if Enum.member?(user["friend_requests"], friend) do
      nfrl = List.delete(user["friend_requests"], friend)
      Mongo.update_one(:mongo, "BackChannel",
                      %{"username" => username},
                      %{"$set" => %{ "friend_requests" => nfrl }},
                      pool: DBConnection.Poolboy)
      IO.inspect true
      else
      IO.inspect false
    end
end
##FRIEND MANAGEMENT##



#EXPENSIVE SHIT
def checkForUpdates(username) do
  newFriendMap = getFriendsMap(username)
  newFriendMap = 
  Enum.reduce(newFriendMap, %{}, fn {k, _v}, out ->
      if(Enum.member?(Map.keys(MongoInterface.getFriendsMap(k)), username) == true) do
        #IO.inspect "#{k} exists"
        Map.put(out, k, "yes")
      else
        #IO.inspect "#{k} does not exist"
        Map.put(out, k, "no")
      end
  end)

    Mongo.update_one(:mongo, "Users",
                      %{"username" => username},
                      %{"$set" => %{ "friends" => [newFriendMap] }},
                      pool: DBConnection.Poolboy)
    newFriendMap
end




##TOKEN MANAGEMENT##
def updateToken(username, token) do
  Mongo.find_one_and_update(:mongo, "Users",
   %{"username" => username},
    %{"$set" => %{ "token" => token }},
     pool: DBConnection.Poolboy)
end

def getToken(target) do
   token = Mongo.find_one(:mongo, "Users",
    %{"username" => target},
     pool: DBConnection.Poolboy)
   token["token"]
end
##TOKEN MANAGEMENT##






##CHANNELS##
def createChannel(channel_name, email, password, channel_desc) do
  Mongo.insert_one(:mongo, "Channels", %{
    "channel_name" => channel_name,
    "email" => email,
    "password" => password,
    "channel_desc" => channel_desc,
    }, pool: DBConnection.Poolboy)
end

def channelExists(channel_name) do
  isContained = Mongo.find_one(:mongo, "Channels", %{"channel_name" => channel_name }, pool: DBConnection.Poolboy)
  case isContained do
    %{"_id" => _id, "channel_name" => _channel_name} -> true
    nil -> false
  end
end
##CHANNELS##

end



