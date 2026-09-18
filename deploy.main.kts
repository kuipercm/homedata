#!/usr/bin/env kotlin

import java.io.File
import java.util.concurrent.TimeUnit

val remoteUser = "nielskuiper"
val remoteHost = "mediaserver.local"
val remoteTarget = "$remoteUser@$remoteHost"

// =============================================================================
// Process & SSH Execution Helpers
// =============================================================================

fun exec(vararg cmd: String, dir: File? = null): Pair<Int, String> {
    val pb = ProcessBuilder(*cmd).redirectErrorStream(true)
    dir?.let { pb.directory(it) }
    val proc = pb.start()
    val out = proc.inputStream.bufferedReader().readText()
    proc.waitFor(10, TimeUnit.MINUTES)
    return proc.exitValue() to out
}

fun execLive(vararg cmd: String, dir: File? = null): Int {
    val pb = ProcessBuilder(*cmd).inheritIO()
    dir?.let { pb.directory(it) }
    val proc = pb.start()
    proc.waitFor(15, TimeUnit.MINUTES)
    return proc.exitValue()
}

fun sshCapture(cmd: String) = exec("ssh", remoteTarget, cmd)
fun sshLive(script: String) = execLive("ssh", remoteTarget, script)
fun sshInteractive(script: String) = execLive("ssh", "-t", remoteTarget, script)

fun fail(msg: String): Nothing {
    System.err.println("\nFAILED: $msg")
    System.exit(1)
    error("unreachable")
}

// =============================================================================
// Storage & Samba Management (/mnt/elements)
// =============================================================================

object StorageManager {
    private const val MOUNT_POINT = "/mnt/elements"
    private const val FSTAB_TAG = "# MANAGED_BY_DEPLOY_TOOL_ELEMENTS"
    private const val SMB_START_TAG = "# BEGIN MANAGED BY DEPLOY-TOOL: ELEMENTS-SMB"
    private const val SMB_END_TAG = "# END MANAGED BY DEPLOY-TOOL: ELEMENTS-SMB"

    fun handle(args: List<String>) {
        val action = args.firstOrNull()?.lowercase() ?: "status"
        when (action) {
            "setup" -> setup()
            "remove", "teardown", "uninstall" -> remove()
            "status" -> status()
            else -> {
                println("Unknown storage action: '$action'")
                println("Usage: ./deploy.main.kts storage [setup|remove|status]")
                System.exit(1)
            }
        }
    }

    private fun setup() {
        println("=== Setting up persistent NTFS mount & Samba share ===")
        val script = """
            set -e

            echo "Step 1: Installing required packages (ntfs-3g, samba)..."
            sudo apt-get update -qq
            sudo apt-get install -y ntfs-3g samba samba-common-bin

            echo -e "\nStep 2: Locating NTFS Elements storage device..."
            TARGET_DEV=""
            if mountpoint -q $MOUNT_POINT 2>/dev/null; then
                TARGET_DEV=$(findmnt -n -o SOURCE $MOUNT_POINT 2>/dev/null || true)
            fi
            if [ -z "${'$'}TARGET_DEV" ]; then
                TARGET_DEV=$(blkid -L Elements 2>/dev/null || blkid -L elements 2>/dev/null || true)
            fi
            if [ -z "${'$'}TARGET_DEV" ]; then
                TARGET_DEV=$(blkid -t TYPE=ntfs -o device 2>/dev/null | head -1 || true)
            fi

            if [ -z "${'$'}TARGET_DEV" ]; then
                echo "ERROR: Could not find an NTFS device or disk labeled 'Elements'."
                echo "Available block devices:"
                lsblk -f
                exit 1
            fi

            UUID=$(blkid -s UUID -o value "${'$'}TARGET_DEV")
            FSTYPE=$(blkid -s TYPE -o value "${'$'}TARGET_DEV")
            echo "Found device: ${'$'}TARGET_DEV (UUID=${'$'}UUID, Type=${'$'}FSTYPE)"

            echo -e "\nStep 3: Configuring /etc/fstab for boot mount..."
            sudo mkdir -p $MOUNT_POINT

            # Remove previous entries for this mount point if present
            sudo sed -i '/$FSTAB_TAG/d' /etc/fstab
            sudo sed -i '\|[[:space:]]$MOUNT_POINT[[:space:]]|d' /etc/fstab

            FSTAB_ENTRY="UUID=${'$'}UUID $MOUNT_POINT ntfs-3g defaults,nofail,uid=1000,gid=1000,umask=0002,windows_names 0 0 $FSTAB_TAG"
            echo "${'$'}FSTAB_ENTRY" | sudo tee -a /etc/fstab > /dev/null
            echo "Added fstab entry with 'nofail' safeguard."

            echo "Mounting $MOUNT_POINT..."
            sudo systemctl daemon-reload
            sudo mount $MOUNT_POINT 2>/dev/null || sudo mount -a

            echo -e "\nStep 4: Configuring Samba share for $MOUNT_POINT..."
            SMB_CONF="/etc/samba/smb.conf"
            sudo sed -i '/$SMB_START_TAG/,/$SMB_END_TAG/d' "${'$'}SMB_CONF"

            cat << 'EOF' | sudo tee -a "${'$'}SMB_CONF" > /dev/null

$SMB_START_TAG
[Elements]
   comment = Elements External Storage
   path = $MOUNT_POINT
   browseable = yes
   read only = no
   guest ok = no
   valid users = $remoteUser
   force user = $remoteUser
   create mask = 0775
   directory mask = 0775
$SMB_END_TAG
EOF

            echo "Restarting Samba daemon..."
            sudo systemctl enable smbd
            sudo systemctl restart smbd

            echo -e "\nStep 5: Checking Samba user '$remoteUser'..."
            if sudo pdbedit -L -u $remoteUser > /dev/null 2>&1; then
                echo "Samba user '$remoteUser' is already configured."
            else
                echo "Please set a Samba password for '$remoteUser':"
                sudo smbpasswd -a $remoteUser
            fi

            echo -e "\n=== Setup Complete! ==="
            df -h $MOUNT_POINT
        """.trimIndent()

        if (sshInteractive(script) != 0) fail("Storage setup failed")
        println("\nStorage and Samba share have been successfully configured.")
    }

    private fun remove() {
        println("=== Removing persistent mount and Samba share for $MOUNT_POINT ===")
        val script = """
            set -e

            SMB_CONF="/etc/samba/smb.conf"
            if [ -f "${'$'}SMB_CONF" ] && grep -q "$SMB_START_TAG" "${'$'}SMB_CONF"; then
                echo "Removing Samba share block..."
                sudo sed -i '/$SMB_START_TAG/,/$SMB_END_TAG/d' "${'$'}SMB_CONF"
                sudo systemctl restart smbd || true
                echo "Samba share removed."
            else
                echo "No managed Samba configuration found."
            fi

            if grep -q "$FSTAB_TAG" /etc/fstab; then
                echo "Removing /etc/fstab entry..."
                sudo sed -i '/$FSTAB_TAG/d' /etc/fstab
                sudo systemctl daemon-reload
                echo "fstab entry removed."
            else
                echo "No managed fstab entry found."
            fi

            if mountpoint -q $MOUNT_POINT; then
                echo "Unmounting $MOUNT_POINT..."
                sudo umount $MOUNT_POINT || echo "Notice: Could not unmount $MOUNT_POINT immediately (may be held by Jellyfin/Docker)."
            fi

            echo "Storage configuration cleanly removed."
        """.trimIndent()

        if (sshLive(script) != 0) fail("Storage removal failed")
        println("\nStorage configuration successfully removed.")
    }

    private fun status() {
        println("=== Storage & Samba Status on $remoteTarget ===")
        val script = """
            echo "--- Mount status for $MOUNT_POINT ---"
            if mountpoint -q $MOUNT_POINT; then
                echo "[MOUNTED]"
                df -h $MOUNT_POINT
            else
                echo "[NOT MOUNTED]"
            fi

            echo -e "\n--- /etc/fstab managed entry ---"
            grep "$FSTAB_TAG" /etc/fstab || echo "(none)"

            echo -e "\n--- Samba service status ---"
            sudo systemctl is-active smbd >/dev/null 2>&1 && echo "smbd: running" || echo "smbd: not running"

            echo -e "\n--- Active Samba shares ---"
            sudo smbstatus --shares 2>/dev/null || true
        """.trimIndent()

        sshLive(script)
    }
}

// =============================================================================
// Service Deployment Abstractions
// =============================================================================

interface ServiceDeployer {
    val name: String
    val description: String
    fun deploy(args: List<String>)
}

/**
 * Generic deployer for standalone Docker images (e.g. from Docker Hub or GHCR).
 * Handles pulling the image, gracefully stopping/removing any existing container with the same name,
 * and starting the updated container with configured flags and mounts.
 */
class DockerImageDeployer(
    override val name: String,
    override val description: String,
    val containerName: String,
    val defaultImage: String,
    val runArgs: List<String>,
    val pullBeforeRun: Boolean = true
) : ServiceDeployer {
    override fun deploy(args: List<String>) {
        val targetImage = if (args.isNotEmpty()) {
            val customTag = args[0]
            if (customTag.contains(":") || customTag.contains("/")) {
                customTag
            } else {
                "${defaultImage.substringBefore(":")}:$customTag"
            }
        } else {
            defaultImage
        }

        println("=== Deploying $name ($containerName) ===")
        println("Target image: $targetImage")

        val pullStep = if (pullBeforeRun) """
            echo "Pulling image '$targetImage'..."
            docker pull $targetImage
        """.trimIndent() else ""

        val script = """
            set -e
            $pullStep

            if docker ps -a --format '{{.Names}}' | grep -Eq '^${containerName}$'; then
                echo "Disabling restart policy on existing container '$containerName'..."
                docker update --restart no $containerName 2>/dev/null || true
                echo "Stopping existing '$containerName' container..."
                docker stop $containerName
                echo "Removing existing '$containerName' container..."
                docker rm $containerName
            fi

            echo "Starting new container '$containerName'..."
            docker run -d \
                --name $containerName \
                ${runArgs.joinToString(" \\\n                ")} \
                $targetImage

            echo -e "\nVerifying container status..."
            docker ps --filter name="^${containerName}$" --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Names}}"
        """.trimIndent()

        if (sshLive(script) != 0) fail("Deployment of $name failed")
        println("\nDone! $containerName ($targetImage) is running.")
    }
}

/**
 * Deployer for the local Housedata Spring Boot application.
 * Builds via Maven, SCPs jar to the remote machine, builds Docker image on remote, and restarts container.
 */
class HousedataDeployer : ServiceDeployer {
    override val name = "housedata"
    override val description = "Housedata IoT power tracking Spring Boot app"

    private val remoteDir = "~/housedata"
    private val snapshotJar = "housedata-0.0.1-SNAPSHOT.jar"
    private val targetJar = "housedata.jar"
    private val imageName = "housedata"

    override fun deploy(args: List<String>) {
        println("=== Building project ===")
        if (execLive("mvn", "clean", "verify") != 0) fail("Maven build failed")

        println("\n=== Copying jar to remote ===")
        if (execLive("scp", "target/$snapshotJar", "$remoteTarget:$remoteDir/") != 0)
            fail("SCP failed")

        println("\n=== Getting running container info ===")
        val (psCode, psOutput) = sshCapture(
            "docker ps --format '{{.ID}} {{.Image}}' | grep $imageName | head -1"
        )

        val runningContainerId: String?
        val currentImage: String?
        if (psCode == 0 && psOutput.isNotBlank()) {
            val parts = psOutput.trim().split(" ")
            runningContainerId = parts[0]
            currentImage = parts[1]
            println("Running Container: $runningContainerId  Image: $currentImage")
        } else {
            runningContainerId = null
            currentImage = null
            println("No currently running '$imageName' container found.")
        }

        val newVersion = if (args.isNotEmpty()) {
            args[0]
        } else if (currentImage != null) {
            val currentVersion = currentImage.substringAfter(":", "0.0.0")
            val versionParts = currentVersion.split(".")
            val patch = versionParts.getOrNull(2)?.toIntOrNull() ?: 0
            "${versionParts.getOrElse(0) { "0" }}.${versionParts.getOrElse(1) { "0" }}.${patch + 1}"
        } else {
            val (imgCode, imgOutput) = sshCapture("docker images --format '{{.Tag}}' $imageName | head -1")
            val latestTag = if (imgCode == 0 && imgOutput.isNotBlank()) imgOutput.trim() else "0.0.0"
            val versionParts = latestTag.split(".")
            val patch = versionParts.getOrNull(2)?.toIntOrNull() ?: 0
            "${versionParts.getOrElse(0) { "0" }}.${versionParts.getOrElse(1) { "0" }}.${patch + 1}"
        }
        println("New image: $imageName:$newVersion")

        println("\n=== Deploying on remote ===")
        val stopContainerScript = if (runningContainerId != null) """
            echo "Disabling restart policy on $runningContainerId..."
            docker update --restart no $runningContainerId
            echo "Stopping container..."
            docker stop $runningContainerId
        """.trimIndent() else """
            echo "No previous container to stop."
        """.trimIndent()

        val exitCode = sshLive("""
            set -e
            cd $remoteDir
            $stopContainerScript
            echo "Swapping jar..."
            rm -f $targetJar
            cp $snapshotJar $targetJar
            rm -f $snapshotJar
            echo "Building new image..."
            docker build --tag $imageName:$newVersion .
            echo "Starting new container..."
            docker run -d --network host --restart always \
                --volume ~/housedata/emergency_products:/home/root/app \
                --env housedata.urls.WASHING_MACHINE=http://192.168.2.4 \
                --env housedata.urls.DRYER=http://192.168.2.5 \
                $imageName:$newVersion
        """.trimIndent())

        if (exitCode != 0) fail("Remote deployment failed")

        println("\nDone! $imageName:$newVersion is running.")
    }
}

// =============================================================================
// Service Registry
// To add a new service, instantiate a DockerImageDeployer below.
// =============================================================================

val services: List<ServiceDeployer> = listOf(
    HousedataDeployer(),
    DockerImageDeployer(
        name = "jellyfin",
        description = "Jellyfin media server",
        containerName = "jellyfin",
        defaultImage = "jellyfin/jellyfin:latest",
        runArgs = listOf(
            "--net=host",
            "--volume jellyfin-config:/config",
            "--volume jellyfin-cache:/cache",
            "--mount type=bind,source=/mnt/elements/Media,target=/media",
            "--restart=always"
        )
    )
)

val serviceMap: Map<String, ServiceDeployer> = services.associateBy { it.name.lowercase() }

fun printUsage() {
    println("Generic Remote Docker Deployment & Host Management Tool")
    println("Target Host: $remoteTarget")
    println("\nUsage:")
    println("  ./deploy.main.kts [service] [options/version]")
    println("  ./deploy.main.kts storage [setup|remove|status]")
    println("\nConfigured Services:")
    for (s in services) {
        println("  %-14s %s".format(s.name, s.description))
    }
    println("\nStorage Commands:")
    println("  storage setup    Configure persistent NTFS mount (/mnt/elements) & Samba share")
    println("  storage remove   Remove persistent mount from fstab and Samba share")
    println("  storage status   Check /mnt/elements mount and Samba daemon status")
    println("\nExamples:")
    println("  ./deploy.main.kts storage setup      Set up drive & SMB share on the Pi")
    println("  ./deploy.main.kts storage status     Check drive & SMB status")
    println("  ./deploy.main.kts storage remove     Cleanly teardown drive & SMB share")
    println("  ./deploy.main.kts jellyfin           Deploy latest Jellyfin container")
    println("  ./deploy.main.kts jellyfin 10.9.1    Deploy specific Jellyfin tag")
    println("  ./deploy.main.kts housedata          Build & deploy housedata (auto-bump patch)")
    println("  ./deploy.main.kts housedata 0.0.5    Build & deploy housedata with version 0.0.5")
    println("  ./deploy.main.kts                    Defaults to 'housedata'")
}

// =============================================================================
// CLI Entrypoint
// =============================================================================

if (args.isNotEmpty() && (args[0] == "-h" || args[0] == "--help" || args[0] == "help" || args[0] == "list")) {
    printUsage()
    System.exit(0)
}

if (args.isNotEmpty() && args[0].lowercase() == "storage") {
    StorageManager.handle(args.drop(1))
    System.exit(0)
}

val (selectedService, remainingArgs) = when {
    args.isEmpty() -> {
        println("No service specified. Defaulting to 'housedata'. (Use --help to list available services)")
        serviceMap["housedata"]!! to emptyList()
    }
    serviceMap.containsKey(args[0].lowercase()) -> {
        serviceMap[args[0].lowercase()]!! to args.drop(1)
    }
    args[0].first().isDigit() -> {
        // Backwards compatibility: if argument starts with a number (e.g. 0.0.5), assume it's a version for housedata
        println("No service specified; treating '${args[0]}' as version for 'housedata'.")
        serviceMap["housedata"]!! to listOf(args[0])
    }
    else -> {
        System.err.println("Error: Unknown service '${args[0]}'")
        println()
        printUsage()
        System.exit(1)
        error("unreachable")
    }
}

selectedService.deploy(remainingArgs)
