var app = require('express')();
var http = require('http').createServer(app);
var io = require('socket.io')(http);
const AppDAO = require('./dao');
const RoomRepository = require('./room_repository');
var roomRepo;
var curTalking = {};

function startup() {
	const dao = new AppDAO('./database.sqlite3');
	roomRepo = new RoomRepository(dao);
}

app.get('/', (req, res) => {
	res.sendFile(__dirname + '/index.html');
});

io.on('connection', (client) => {

	// Client joining a room
	client.on('join', async (data) => {
		let roomDBEntry = await roomRepo.createTable()
			.then(() => roomRepo.findRoom({
				username: data.username,
				spoke: data.spoke
			}));

		if (roomDBEntry) {
			// Leave all rooms
			client.leaveAll();

			// Update database with new room
			console.log(`------------[ ${data.username} moved to ${data.room} ]`);
			await roomRepo.updateRoom(data);
			client.join(data.room);
			io.to(data.room).emit("joinedRoom", data.username);
			io.to(roomDBEntry.room).emit("leftRoom", roomDBEntry);
		}
		else {
			// Insert database with room
			await roomRepo.insertRoom(data).then(() => {
				console.log(`------------[ ${data.username} joined ${data.room} ]`);
				client.join(data.room);
				io.to(data.room).emit("joinedRoom", data.username);
			});
		}

		// Join their own room (used for private 1-1 voice chat)
		client.join(data.username+"#"+data.spoke);
	});

	// Get ROOM Participants
	client.on('getRoomInfo', async (data) => {
		let roomMembers = await roomRepo.allInRoom(data);
		console.log('ROOM INFO REQUESTED', roomMembers);
		io.to(data.room).emit('roomMembers', roomMembers);
	});

	// Client exiting
	client.on('exit', async (data) => {
		console.log('exit', data);
		await roomRepo.leaveRoom(data);

		let roomMembers = await roomRepo.allInRoom(data);
		console.log('ROOM INFO REQUESTED', roomMembers);
		io.to(data.room).emit('roomMembers', roomMembers);

		client.leaveAll();
	});

	// Client disconnecting
	client.on('disconnect', (data) => {
		console.log('disconnect');
		client.leaveAll();
	});

	// Client stream to room
	client.on('streamingToServer', (data) => {
		if (!curTalking || !curTalking.hasOwnProperty(data.room)) {
			curTalking[data.room] = data.username;
			client.emit('serverReady', data.username);
			console.log(data.username + ' is talking in ' + data.room);
		} else if (curTalking[data.room] == data.username) {
			client.to(data.room).emit('streamingToClient', {
				username: data.username,
			}, data.buffer);
		} else {
			console.log(data.username + ' not allowed to talk in ' + data.room);
			client.emit("talkNotGranted");
		}

	});

	// client ends stream to room
	client.on('endServerStream', (data) => {
		if (curTalking[data.room] == data.username) {
			console.log(data.username + ' has finished talking in ' + data.room);
			delete(curTalking[data.room]);
			client.to(data.room).emit('endClientStream', data);
			client.to(data.room).emit('serverReady', null);
		}
	});





	// PRIVATE CHAT

	// client joining a private room
	client.on('joinPrivate', async (data) => {
		console.log(data.username + " joined " + data.room);
		client.join(data.room);
	});

	// client leaving a private room
	client.on('leavePrivate', async (data) => {
		console.log(data.username + " left " + data.room);
		client.leave(data.room);
	});

	// client initiates private room for selection
	client.on('privateInit', (data) => {
		console.log('private initizalized', data);
		io.sockets.emit('invitedPrivate', data); // send all users a list of the selected users for private chat
	});

	// client streams privately to selection
	client.on('privateStreaming', (data) => {
		if (!curTalking || curTalking[data.room] != data.username) {
			curTalking[data.room] = data.username;
			client.emit('serverReady', data.username);
			console.log(data.username + ' is talking in ' + data.room);
		}
		client.to(data.room).emit('streamingToClient', {
			username: data.username,
		}, data.buffer);
	});

	// client ends private stream
	client.on('privateEndStream', (data) => {
		console.log(data.username + ' has finished talking in ' + data.room);
		delete(curTalking[data.room]);
		console.log(curTalking);
		client.to(data.room).emit('endPrivateClientStream', data);
		client.to(data.room).emit('serverReady', null);
	});
});

http.listen(3000, () => {
	console.log('listening on *:3000');
	startup();
});
