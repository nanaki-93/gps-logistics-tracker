package config

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestPostGISComposeDoesNotForceAMD64Platform(t *testing.T) {
	composePath := filepath.Join("..", "..", "docker-compose.yml")
	data, err := os.ReadFile(composePath)
	if err != nil {
		t.Fatalf("read %s: %v", composePath, err)
	}

	section := serviceSection(strings.ReplaceAll(string(data), "\r\n", "\n"), "logistics-db")
	if section == "" {
		t.Fatal("logistics-db service not found in docker-compose.yml")
	}
	if strings.Contains(section, "platform: linux/amd64") {
		t.Fatalf("logistics-db forces linux/amd64 emulation:\n%s", section)
	}
}

func serviceSection(compose, service string) string {
	header := "  " + service + ":\n"
	_, after, ok := strings.Cut(compose, header)
	if !ok {
		return ""
	}
	var lines []string
	for _, line := range strings.Split(after, "\n") {
		if line != "" && !strings.HasPrefix(line, "    ") {
			break
		}
		lines = append(lines, line)
	}
	return strings.Join(lines, "\n")
}
