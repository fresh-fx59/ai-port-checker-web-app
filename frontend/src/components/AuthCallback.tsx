import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

function AuthCallback() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { refreshUser } = useAuth();

  useEffect(() => {
    const handleCallback = async () => {
      const token = searchParams.get('token');
      const error = searchParams.get('error');

      if (error) {
        console.error('OAuth error:', error);
        navigate('/login?error=' + encodeURIComponent(error));
        return;
      }

      if (token) {
        // Store the token and refresh user data
        localStorage.setItem('authToken', token);
        await refreshUser();
        navigate('/');
      } else {
        navigate('/login?error=no_token');
      }
    };

    handleCallback();
  }, [searchParams, navigate, refreshUser]);

  return (
    <div className="max-w-md mx-auto text-center">
      <div className="bg-white shadow-md rounded-lg p-6">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <h2 className="text-xl font-semibold text-gray-900 mb-2">
          Completing Authentication
        </h2>
        <p className="text-gray-600">
          Please wait while we complete your sign-in...
        </p>
      </div>
    </div>
  );
}

export default AuthCallback;