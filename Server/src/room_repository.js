class RoomRepository {
	constructor(dao) {
		this.dao = dao
	}

	createTable() {
		const sql = `CREATE TABLE IF NOT EXISTS rooms (
		username TEXT PRIMARY KEY,
		room TEXT NOT NULL,
		spoke TEXT NOT NULL)`
		return this.dao.run(sql)
	}

	findRoom(data) {
		return this.dao.get(
			`SELECT * FROM rooms WHERE username = ? AND spoke = ?`,
			[data.username, data.spoke])
	}

	insertRoom(data) {
		return this.dao.run(
			'INSERT INTO rooms (username, room, spoke) VALUES (?, ?, ?)',
			[data.username, data.room, data.spoke])
	}

	updateRoom(data) {
		return this.dao.run(
			'UPDATE rooms SET room = ?, spoke = ? WHERE username = ?',
			[data.room, data.spoke, data.username])
	}

	leaveRoom(data) {
		return this.dao.run(
			`DELETE FROM rooms WHERE username = ? AND spoke = ?`,
			[data.username, data.spoke]
		)
	}

	allInRoom(data) {
		return this.dao.all(
			`SELECT username FROM rooms WHERE room = ?`,
			[data.room]
		)
	}


	getAll() {
		return this.dao.all(`SELECT * FROM rooms`)
	}
}

module.exports = RoomRepository;