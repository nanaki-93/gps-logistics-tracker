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
	if !strings.Contains(section, "${POSTGIS_IMAGE:-postgis/postgis:15-3.3}") {
		t.Fatalf("logistics-db does not parameterize POSTGIS_IMAGE:\n%s", section)
	}
}

func TestComposeDoesNotHardcodeCredentialsOrExposeDatastorePorts(t *testing.T) {
	composePath := filepath.Join("..", "..", "docker-compose.yml")
	data, err := os.ReadFile(composePath)
	if err != nil {
		t.Fatalf("read %s: %v", composePath, err)
	}
	compose := strings.ReplaceAll(string(data), "\r\n", "\n")

	checks := []struct {
		service   string
		forbidden []string
		required  []string
	}{
		{
			service:   "logistics-db",
			forbidden: []string{"POSTGRES_PASSWORD: password", `"5432:5432"`},
			required:  []string{"${POSTGRES_PASSWORD:-password}", `"127.0.0.1:5432:5432"`},
		},
		{
			service:   "logistics-cache",
			forbidden: []string{"--requirepass yourpassword", `"6379:6379"`},
			required:  []string{"--requirepass ${REDIS_PASSWORD:-yourpassword}", `"127.0.0.1:6379:6379"`},
		},
		{
			service:   "telemetry-mw-rabbitmq",
			forbidden: []string{"RABBITMQ_DEFAULT_PASS: password", `"5672:5672"`},
			required:  []string{"${RABBITMQ_PASSWORD:-password}", `"127.0.0.1:5672:5672"`},
		},
		{
			service:   "telemetry-service",
			forbidden: []string{"admin:password@", "API_KEYS: key-truck-001,key-truck-002"},
			required:  []string{"${RABBITMQ_PASSWORD:-password}", "${API_KEYS:-key-truck-001,key-truck-002}"},
		},
		{
			service:   "logistics-service",
			forbidden: []string{"POSTGRES_PASSWORD: password", "RABBITMQ_PASSWORD: password"},
			required:  []string{"${POSTGRES_PASSWORD:-password}", "${RABBITMQ_PASSWORD:-password}", "${REDIS_PASSWORD:-yourpassword}"},
		},
	}

	for _, tc := range checks {
		assertServiceSection(t, compose, tc.service, tc.forbidden, tc.required)
	}
}

func assertServiceSection(t *testing.T, compose, service string, forbidden, required []string) {
	t.Helper()
	section := serviceSection(compose, service)
	if section == "" {
		t.Fatalf("%s service not found in docker-compose.yml", service)
	}
	for _, s := range forbidden {
		if strings.Contains(section, s) {
			t.Errorf("%s contains forbidden literal %q:\n%s", service, s, section)
		}
	}
	for _, s := range required {
		if !strings.Contains(section, s) {
			t.Errorf("%s missing expected config %q:\n%s", service, s, section)
		}
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
