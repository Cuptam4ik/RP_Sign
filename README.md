
---

### 📝 Summary

**RP\_Sign** is a lightweight Minecraft server plugin that allows you to manage resource packs via signs.
It provides the `/rp` command for creating, deleting, listing, and updating resource packs directly from your server

---

### 💡 Features

* Assign resource packs to signs for quick installation by players
* Full support for **Paper**, **Purpur**, **Spigot**, and **Folia**
* Compatible with **Minecraft 1.21+**
* Easy configuration via `config.yml` and `packs.yml`
* Intuitive `/rp` command with multiple subcommands:

  * `create` — Create a new resource pack
  * `delete` — Remove a resource pack
  * `list` — Show all available packs
  * `clear` — Clear all packs
  * `reload` — Reload configuration files
  * `lang` — Change plugin language
* Automatic resource pack delivery to players via commands or signs

---

### 🪧 Sign Format

To create a resource pack sign:

**Example:**

```
[RP]
Pack_name
Click to install!
```

When a player right-clicks this sign, the plugin will automatically send them the linked resource pack.

---

### 🔧 Compatibility

* **Minecraft versions:** 1.21+
* **Server platforms:** Bukkit/Spigot/Paper/Purpur/Folia
* **Folia:** Fully supported

---

### 📚 Commands

| Command      | Description            |
| ------------ | ---------------------- |
| `/rp create` | Create a resource pack |
| `/rp delete` | Delete a resource pack |
| `/rp list`   | List available packs   |
| `/rp help`   | Show help              |
| `/rp clear`  | Clear the pack list    |
| `/rp reload` | Reload settings        |
| `/rp lang`   | Change plugin language |

---


---

